package com.example.digitalenterpriseservices.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatStreamEvent {
    public enum Type {
        TOKEN,
        TOOL_CALL,
        TOOL_RESULT,
        DONE,
        UNKNOWN
    }

    Type type;
    String content;
}
