package com.example.digitalenterpriseservices.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    private String sessionId;
    @NotBlank(message = "Message is required")
    private String message;
    private UiFilters uiFilters;
}
