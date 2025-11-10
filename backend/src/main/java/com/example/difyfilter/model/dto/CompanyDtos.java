package com.example.difyfilter.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collections;
import java.util.List;

public final class CompanyDtos {

    private CompanyDtos() {
    }

    public record CompanySearchRequest(
            String q,
            List<String> industry,
            List<String> size,
            List<String> region,
            List<String> tech,
            Integer limit
    ) {
        public int resolveLimit(int defaultValue) {
            return limit == null || limit <= 0 ? defaultValue : Math.min(limit, 100);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CompanyCard(
            String id,
            String name,
            List<String> industry,
            String size,
            List<String> region,
            List<String> techKeywords,
            String summary,
            double score,
            String reason
    ) {
        public CompanyCard withReason(String newReason) {
            return new CompanyCard(id, name, industry, size, region, techKeywords, summary, score, newReason);
        }

        public List<String> safeIndustry() {
            return industry == null ? Collections.emptyList() : industry;
        }

        public List<String> safeRegion() {
            return region == null ? Collections.emptyList() : region;
        }

        public List<String> safeTechKeywords() {
            return techKeywords == null ? Collections.emptyList() : techKeywords;
        }
    }
}
