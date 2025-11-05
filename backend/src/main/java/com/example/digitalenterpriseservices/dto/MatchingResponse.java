package com.example.digitalenterpriseservices.dto;

import java.util.List;

public record MatchingResponse(
        String summary,
        List<SuggestedEnterprise> suggestions
) {

    public record SuggestedEnterprise(
            String name,
            String description,
            String score,
            String contact
    ) {
    }
}
