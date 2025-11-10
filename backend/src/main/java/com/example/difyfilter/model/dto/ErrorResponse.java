package com.example.difyfilter.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        String traceId,
        int status,
        String error,
        String message,
        String path
) {
}
