package com.example.difyfilter.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public final class ChatDtos {

    private ChatDtos() {
    }

    public enum StreamEventType {
        TOKEN,
        COMPANIES,
        DONE,
        CONVERSATION,
        TOOL_CALL
    }

    public record ChatStreamEvent(StreamEventType type, String data, ToolCallPayload toolCallPayload, JsonNode raw) {
        public static ChatStreamEvent token(String token, JsonNode raw) {
            return new ChatStreamEvent(StreamEventType.TOKEN, token, null, raw);
        }

        public static ChatStreamEvent done(JsonNode raw) {
            return new ChatStreamEvent(StreamEventType.DONE, null, null, raw);
        }

        public static ChatStreamEvent conversation(String conversationId, JsonNode raw) {
            return new ChatStreamEvent(StreamEventType.CONVERSATION, conversationId, null, raw);
        }

        public static ChatStreamEvent toolCall(ToolCallPayload payload, JsonNode raw) {
            return new ChatStreamEvent(StreamEventType.TOOL_CALL, null, payload, raw);
        }
    }

    public record ToolCallPayload(
            String messageId,
            String toolCallId,
            String toolName,
            Map<String, Object> arguments
    ) {
    }

    public record ChatStreamRequest(
            String message,
            String sessionId,
            String conversationId,
            Map<String, List<String>> filters
    ) {
    }
}
