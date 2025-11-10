package com.example.digitalenterpriseservices.client.dify;

import com.example.digitalenterpriseservices.config.AppProperties;
import com.example.digitalenterpriseservices.model.dto.ChatStreamEvent;
import com.example.digitalenterpriseservices.model.dto.UiFilters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class DifyChatClient {

    private final WebClient difyWebClient;
    private final AppProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public Flux<ChatStreamEvent> streamChat(String conversationId, String message, UiFilters filters, Map<String, Object> toolResult) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("inputs", Map.of());
        payload.put("query", message);
        payload.put("response_mode", "stream");
        payload.put("user", "web-client");
        if (conversationId != null) {
            payload.put("conversation_id", conversationId);
        }
        if (filters != null) {
            Map<String, Object> metadata = new HashMap<>();
            if (filters.getIndustry() != null && !filters.getIndustry().isEmpty()) {
                metadata.put("industry", filters.getIndustry());
            }
            if (filters.getSize() != null && !filters.getSize().isEmpty()) {
                metadata.put("size", filters.getSize());
            }
            if (filters.getRegion() != null && !filters.getRegion().isEmpty()) {
                metadata.put("region", filters.getRegion());
            }
            if (filters.getTech() != null && !filters.getTech().isEmpty()) {
                metadata.put("tech", filters.getTech());
            }
            if (!metadata.isEmpty()) {
                payload.put("metadata", metadata);
            }
        }
        if (toolResult != null) {
            payload.put("tool", toolResult);
        }

        return difyWebClient.post()
                .uri("/v1/chat-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header("Authorization", "Bearer " + properties.getChatAppKey())
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::mapToEvent)
                .onErrorResume(error -> {
                    log.error("Dify chat stream error", error);
                    return Flux.just(ChatStreamEvent.builder()
                            .type(ChatStreamEvent.Type.DONE)
                            .content("[ERROR] " + error.getMessage())
                            .build());
                });
    }

    private ChatStreamEvent mapToEvent(String raw) {
        if (raw == null || raw.isBlank()) {
            return ChatStreamEvent.builder().type(ChatStreamEvent.Type.UNKNOWN).content("").build();
        }
        if ("[DONE]".equals(raw.trim())) {
            return ChatStreamEvent.builder().type(ChatStreamEvent.Type.DONE).content("done").build();
        }
        try {
            JsonNode node = mapper.readTree(raw);
            String event = node.path("event").asText();
            if ("message".equals(event)) {
                String answer = node.path("answer").asText("");
                return ChatStreamEvent.builder()
                        .type(ChatStreamEvent.Type.TOKEN)
                        .content(answer)
                        .build();
            } else if ("message_end".equals(event)) {
                return ChatStreamEvent.builder().type(ChatStreamEvent.Type.DONE).content("done").build();
            } else if ("tool_calls".equals(event) || "tool_call".equals(event)) {
                return ChatStreamEvent.builder()
                        .type(ChatStreamEvent.Type.TOOL_CALL)
                        .content(node.path("tool_input").toString())
                        .build();
            } else if ("error".equals(event)) {
                return ChatStreamEvent.builder()
                        .type(ChatStreamEvent.Type.UNKNOWN)
                        .content(node.path("error").asText())
                        .build();
            }
            return ChatStreamEvent.builder()
                    .type(ChatStreamEvent.Type.UNKNOWN)
                    .content(node.toString())
                    .build();
        } catch (IOException e) {
            return ChatStreamEvent.builder()
                    .type(ChatStreamEvent.Type.TOKEN)
                    .content(raw)
                    .build();
        }
    }
}
