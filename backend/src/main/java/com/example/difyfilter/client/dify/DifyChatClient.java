package com.example.difyfilter.client.dify;

import com.example.difyfilter.model.dto.ChatDtos.ChatStreamEvent;
import com.example.difyfilter.model.dto.ChatDtos.StreamEventType;
import com.example.difyfilter.model.dto.ChatDtos.ToolCallPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DifyChatClient {

    private static final String TOOL_NAME = "pick_companies";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String chatAppKey;
    private final String systemPrompt;

    public DifyChatClient(
            WebClient difyWebClient,
            ObjectMapper objectMapper,
            @Value("${DIFY_CHAT_APP_KEY}") String chatAppKey,
            @Value("classpath:system-prompt.txt") Resource systemPromptResource
    ) throws IOException {
        this.webClient = difyWebClient;
        this.objectMapper = objectMapper;
        this.chatAppKey = chatAppKey;
        this.systemPrompt = StreamUtils.copyToString(systemPromptResource.getInputStream(), StandardCharsets.UTF_8);
    }

    public Flux<ChatStreamEvent> streamChat(String message, String sessionId, String conversationId, Map<String, ?> filters) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", message);
        payload.put("response_mode", "streaming");
        payload.put("user", sessionId != null ? sessionId : UUID.randomUUID().toString());
        if (conversationId != null && !conversationId.isBlank()) {
            payload.put("conversation_id", conversationId);
        }
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("system_prompt", systemPrompt);
        if (filters != null && !filters.isEmpty()) {
            inputs.put("filters", filters);
        }
        payload.put("inputs", inputs);

        return webClient.post()
                .uri("/v1/chat-messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + chatAppKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(BodyInserters.fromValue(payload))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(this::toEvents);
    }

    private Flux<ChatStreamEvent> toEvents(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return Flux.empty();
        }
        String trimmed = rawLine.trim();
        if (trimmed.startsWith("data:")) {
            trimmed = trimmed.substring(5).trim();
        }
        if ("[DONE]".equals(trimmed)) {
            return Flux.just(ChatStreamEvent.done(null));
        }
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            String event = node.path("event").asText();
            if (event == null || event.isBlank()) {
                return Flux.empty();
            }
            switch (event) {
                case "message" -> {
                    String token = node.path("answer").asText();
                    Flux<ChatStreamEvent> tokenFlux = token == null || token.isEmpty()
                            ? Flux.empty()
                            : Flux.just(ChatStreamEvent.token(token, node));
                    String conversationId = node.path("conversation_id").asText(null);
                    if (conversationId != null && !conversationId.isEmpty()) {
                        tokenFlux = Flux.concat(Flux.just(ChatStreamEvent.conversation(conversationId, node)), tokenFlux);
                    }
                    return tokenFlux;
                }
                case "message_end", "workflow_finished" -> {
                    return Flux.just(ChatStreamEvent.done(node));
                }
                case "conversation" -> {
                    String conversationId = node.path("conversation_id").asText();
                    return conversationId == null || conversationId.isEmpty()
                            ? Flux.empty()
                            : Flux.just(ChatStreamEvent.conversation(conversationId, node));
                }
                case "tool_call" -> {
                    ToolCallPayload payload = parseToolCall(node);
                    if (payload != null && TOOL_NAME.equals(payload.toolName())) {
                        return Flux.just(ChatStreamEvent.toolCall(payload, node));
                    }
                }
                default -> {
                }
            }
        } catch (JsonProcessingException e) {
            return Flux.empty();
        }
        return Flux.empty();
    }

    private ToolCallPayload parseToolCall(JsonNode node) {
        JsonNode dataNode = node.path("data");
        if (dataNode.isMissingNode()) {
            dataNode = node;
        }
        String messageId = dataNode.path("message_id").asText(null);
        String toolCallId = dataNode.path("tool_call_id").asText(null);
        String toolName = dataNode.path("tool_name").asText(null);
        Map<String, Object> args = new HashMap<>();
        JsonNode argumentsNode = dataNode.path("arguments");
        if (argumentsNode.isMissingNode() || argumentsNode.isNull()) {
            String rawArguments = dataNode.path("input").asText(null);
            if (rawArguments != null) {
                try {
                    argumentsNode = objectMapper.readTree(rawArguments);
                } catch (JsonProcessingException ignored) {
                }
            }
        }
        if (argumentsNode != null && !argumentsNode.isMissingNode() && !argumentsNode.isNull()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = argumentsNode.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                args.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class));
            }
        }
        if (toolName == null) {
            return null;
        }
        return new ToolCallPayload(messageId, toolCallId, toolName, args);
    }

    public Mono<Void> submitToolResult(String messageId, String toolCallId, Object result) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tool_call_id", toolCallId);
        payload.put("outputs", result);
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1/chat-messages/{messageId}/tool-outputs").build(messageId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + chatAppKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.empty())
                .then();
    }

    public Mono<Void> sendToolError(String messageId, String toolCallId, String error) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tool_call_id", toolCallId);
        payload.put("outputs", Map.of(
                "error", error,
                "encoded", Base64.getEncoder().encodeToString(error.getBytes(StandardCharsets.UTF_8))
        ));
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1/chat-messages/{messageId}/tool-outputs").build(messageId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + chatAppKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.empty())
                .then();
    }

    public Mono<JsonNode> parseJson(String json) {
        try {
            return Mono.just(objectMapper.readTree(json));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
