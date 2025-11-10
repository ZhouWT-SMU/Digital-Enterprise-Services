package com.example.difyfilter.service;

import com.example.difyfilter.client.dify.DifyChatClient;
import com.example.difyfilter.model.dto.ChatDtos.ChatStreamEvent;
import com.example.difyfilter.model.dto.ChatDtos.ChatStreamRequest;
import com.example.difyfilter.model.dto.CompanyDtos.CompanyCard;
import com.example.difyfilter.model.dto.CompanyDtos.CompanySearchRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ChatService {

    private final DifyChatClient chatClient;
    private final CompanySearchService companySearchService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> sessionConversation = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> sessionBuffers = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionCompanies = new ConcurrentHashMap<>();

    public ChatService(DifyChatClient chatClient,
                       CompanySearchService companySearchService,
                       ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.companySearchService = companySearchService;
        this.objectMapper = objectMapper;
    }

    public Flux<ServerSentEvent<String>> stream(ChatStreamRequest request) {
        String sessionId = request.sessionId() != null && !request.sessionId().isBlank()
                ? request.sessionId()
                : UUID.randomUUID().toString();
        String conversationId = sessionConversation.get(sessionId);
        sessionCompanies.put(sessionId, Boolean.FALSE);
        sessionBuffers.put(sessionId, new StringBuilder());

        Flux<ChatStreamEvent> eventFlux = chatClient.streamChat(
                request.message(),
                sessionId,
                conversationId,
                request.filters());

        Flux<ServerSentEvent<String>> sessionEvent = Flux.just(ServerSentEvent.<String>builder()
                .event("session")
                .data(sessionId)
                .build());

        return Flux.concat(sessionEvent, eventFlux.flatMap(event -> handleEvent(sessionId, request, event)))
                .onErrorResume(ex -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data(ex.getMessage())
                        .build()));
    }

    private Flux<ServerSentEvent<String>> handleEvent(String sessionId, ChatStreamRequest request, ChatStreamEvent event) {
        switch (event.type()) {
            case TOKEN -> {
                appendBuffer(sessionId, event.data());
                return Flux.just(ServerSentEvent.<String>builder()
                        .event("token")
                        .data(event.data())
                        .build());
            }
            case CONVERSATION -> {
                sessionConversation.put(sessionId, event.data());
                return Flux.empty();
            }
            case TOOL_CALL -> {
                return handleToolCall(sessionId, request, event);
            }
            case DONE -> {
                return onCompletion(sessionId, request, event.raw());
            }
            default -> {
                return Flux.empty();
            }
        }
    }

    private void appendBuffer(String sessionId, String token) {
        sessionBuffers.computeIfAbsent(sessionId, key -> new StringBuilder()).append(token);
    }

    private Flux<ServerSentEvent<String>> handleToolCall(String sessionId, ChatStreamRequest request, ChatStreamEvent event) {
        var payload = event.toolCallPayload();
        CompanySearchRequest searchRequest = buildSearchRequest(payload.arguments(), request);
        return companySearchService.search(searchRequest)
                .flatMapMany(cards -> {
                    sessionCompanies.put(sessionId, Boolean.TRUE);
                    return chatClient.submitToolResult(payload.messageId(), payload.toolCallId(), Map.of("companies", cards))
                            .thenMany(Flux.just(companiesEvent(cards)));
                })
                .onErrorResume(ex -> chatClient.sendToolError(payload.messageId(), payload.toolCallId(), ex.getMessage())
                        .thenMany(Flux.just(ServerSentEvent.<String>builder()
                                .event("error")
                                .data("工具调用失败: " + ex.getMessage())
                                .build())));
    }

    private CompanySearchRequest buildSearchRequest(Map<String, Object> arguments, ChatStreamRequest request) {
        Map<String, Object> safeArgs = arguments == null ? Map.of() : arguments;
        Map<String, List<String>> requestFilters = request.filters() == null ? Map.of() : request.filters();
        List<String> industry = toList(safeArgs.getOrDefault("industry", requestFilters.get("industry")));
        List<String> size = toList(safeArgs.getOrDefault("size", requestFilters.get("size")));
        List<String> region = toList(safeArgs.getOrDefault("region", requestFilters.get("region")));
        List<String> tech = toList(safeArgs.getOrDefault("tech", requestFilters.get("tech")));
        String q = (String) safeArgs.getOrDefault("q", request.message());
        Integer limit = safeArgs.get("limit") instanceof Number number ? number.intValue() : 20;
        return new CompanySearchRequest(q, industry, size, region, tech, limit);
    }

    private List<String> toList(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return List.of(value.toString());
    }

    private Flux<ServerSentEvent<String>> onCompletion(String sessionId, ChatStreamRequest request, JsonNode raw) {
        boolean companiesSent = sessionCompanies.getOrDefault(sessionId, Boolean.FALSE);
        Flux<ServerSentEvent<String>> doneFlux = Flux.just(ServerSentEvent.<String>builder().event("done").data("done").build());
        if (!companiesSent) {
            String content = sessionBuffers.getOrDefault(sessionId, new StringBuilder()).toString();
            return extractCompaniesFromFilters(content, request)
                    .filter(cards -> !cards.isEmpty())
                    .flatMapMany(cards -> Flux.concat(
                            Flux.just(companiesEvent(cards)),
                            doneFlux))
                    .switchIfEmpty(doneFlux)
                    .doFinally(signalType -> cleanupSession(sessionId));
        }
        cleanupSession(sessionId);
        return doneFlux;
    }

    private void cleanupSession(String sessionId) {
        sessionBuffers.remove(sessionId);
        sessionCompanies.remove(sessionId);
    }

    private ServerSentEvent<String> companiesEvent(List<CompanyCard> cards) {
        try {
            return ServerSentEvent.<String>builder()
                    .event("companies")
                    .data(objectMapper.writeValueAsString(cards))
                    .build();
        } catch (JsonProcessingException e) {
            return ServerSentEvent.<String>builder()
                    .event("error")
                    .data("公司数据序列化失败")
                    .build();
        }
    }

    private Mono<List<CompanyCard>> extractCompaniesFromFilters(String content, ChatStreamRequest request) {
        int start = content.lastIndexOf("<FILTERS>");
        int end = content.lastIndexOf("</FILTERS>");
        if (start == -1 || end == -1 || end <= start) {
            return Mono.empty();
        }
        String json = content.substring(start + "<FILTERS>".length(), end);
        try {
            Map<String, Object> filters = objectMapper.readValue(json, Map.class);
            CompanySearchRequest searchRequest = buildSearchRequest(filters, request);
            return companySearchService.search(searchRequest);
        } catch (Exception e) {
            return Mono.empty();
        }
    }
}
