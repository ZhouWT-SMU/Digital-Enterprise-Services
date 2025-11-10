package com.example.digitalenterpriseservices.service;

import com.example.digitalenterpriseservices.client.dify.DifyChatClient;
import com.example.digitalenterpriseservices.model.dto.ChatRequest;
import com.example.digitalenterpriseservices.model.dto.ChatStreamEvent;
import com.example.digitalenterpriseservices.model.dto.UiFilters;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Pattern FILTER_PATTERN = Pattern.compile("<FILTERS>(.*?)</FILTERS>", Pattern.DOTALL);

    private final DifyChatClient chatClient;
    private final CompanySearchService companySearchService;
    private final SessionService sessionService;
    private final ObjectMapper mapper = new ObjectMapper();

    public Flux<ServerSentEvent<String>> streamChat(ChatRequest request) {
        String sessionId = Optional.ofNullable(request.getSessionId()).orElseGet(() -> java.util.UUID.randomUUID().toString());
        String conversationId = sessionService.getOrCreateConversation(sessionId);
        AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());
        AtomicBoolean companiesSent = new AtomicBoolean(false);

        Flux<ChatStreamEvent> rawStream = chatClient.streamChat(conversationId, request.getMessage(), request.getUiFilters(), null);

        return rawStream.flatMapSequential(event -> {
            if (event.getType() == ChatStreamEvent.Type.TOKEN) {
                buffer.get().append(event.getContent());
                return Flux.concat(
                        Flux.just(ServerSentEvent.<String>builder()
                                .event("token")
                                .data(event.getContent())
                                .build()),
                        emitCompaniesIfNecessary(buffer.get(), companiesSent, request.getUiFilters(), request.getMessage())
                );
            } else if (event.getType() == ChatStreamEvent.Type.TOOL_CALL) {
                return Flux.from(handleToolCall(event.getContent(), request.getUiFilters()));
            } else if (event.getType() == ChatStreamEvent.Type.DONE) {
                return Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("done")
                        .build());
            }
            return Flux.empty();
        });
    }

    private Flux<ServerSentEvent<String>> emitCompaniesIfNecessary(StringBuilder buffer, AtomicBoolean companiesSent,
                                                                   UiFilters uiFilters, String query) {
        if (companiesSent.get()) {
            return Flux.empty();
        }
        Matcher matcher = FILTER_PATTERN.matcher(buffer.toString());
        if (matcher.find()) {
            String json = matcher.group(1);
            companiesSent.set(true);
            UiFilters parsedFilters = parseFilters(json, uiFilters);
            return companySearchService.searchCompanies(query, parsedFilters, 20)
                    .flatMapMany(cards -> {
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("companies")
                                .data(writeJson(buildCompaniesPayload(parsedFilters, cards)))
                                .build());
                    });
        }
        return Flux.empty();
    }

    private Mono<ServerSentEvent<String>> handleToolCall(String payload, UiFilters fallbackFilters) {
        UiFilters filters = parseFilters(payload, fallbackFilters);
        return companySearchService.searchCompanies(null, filters, 20)
                .map(cards -> ServerSentEvent.<String>builder()
                        .event("companies")
                        .data(writeJson(buildCompaniesPayload(filters, cards)))
                        .build());
    }

    private Map<String, Object> buildCompaniesPayload(UiFilters filters, List<?> cards) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("filters", filters);
        payload.put("companies", cards);
        return payload;
    }

    private UiFilters parseFilters(String json, UiFilters fallback) {
        try {
            UiFilters filters = mapper.readValue(json, UiFilters.class);
            if (filters.getIndustry() == null && fallback != null) {
                filters.setIndustry(fallback.getIndustry());
            }
            if (filters.getSize() == null && fallback != null) {
                filters.setSize(fallback.getSize());
            }
            if (filters.getRegion() == null && fallback != null) {
                filters.setRegion(fallback.getRegion());
            }
            if (filters.getTech() == null && fallback != null) {
                filters.setTech(fallback.getTech());
            }
            return filters;
        } catch (JsonProcessingException e) {
            return Optional.ofNullable(fallback).orElseGet(UiFilters::new);
        }
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize SSE payload", e);
        }
    }
}
