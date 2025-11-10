package com.example.digitalenterpriseservices.service;

import com.example.digitalenterpriseservices.client.dify.DifyDatasetClient;
import com.example.digitalenterpriseservices.model.DatasetDocument;
import com.example.digitalenterpriseservices.model.dto.CompanyCard;
import com.example.digitalenterpriseservices.model.dto.DatasetQuery;
import com.example.digitalenterpriseservices.model.dto.UiFilters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CompanySearchService {

    private final DifyDatasetClient datasetClient;
    public Mono<List<CompanyCard>> searchCompanies(String query, UiFilters filters, Integer limit) {
        int resolvedLimit = Optional.ofNullable(limit).filter(l -> l > 0).orElse(20);
        Map<String, List<String>> metadata = buildMetadata(filters);

        DatasetQuery datasetQuery = DatasetQuery.builder()
                .query(buildQueryWithFallback(query, metadata))
                .limit(resolvedLimit)
                .metadata(metadata)
                .build();

        return datasetClient.search(datasetQuery)
                .map(results -> filterAndTransform(results, metadata, resolvedLimit));
    }

    private Map<String, List<String>> buildMetadata(UiFilters filters) {
        Map<String, List<String>> metadata = new HashMap<>();
        if (filters == null) {
            return metadata;
        }
        if (!CollectionUtils.isEmpty(filters.getIndustry())) {
            metadata.put("industry", filters.getIndustry());
        }
        if (!CollectionUtils.isEmpty(filters.getSize())) {
            metadata.put("size", filters.getSize());
        }
        if (!CollectionUtils.isEmpty(filters.getRegion())) {
            metadata.put("region", filters.getRegion());
        }
        if (!CollectionUtils.isEmpty(filters.getTech())) {
            metadata.put("tech", filters.getTech());
        }
        return metadata;
    }

    private String buildQueryWithFallback(String query, Map<String, List<String>> metadata) {
        StringBuilder builder = new StringBuilder();
        if (query != null && !query.isBlank()) {
            builder.append(query);
        }
        if (!metadata.isEmpty()) {
            builder.append(" ");
            metadata.forEach((key, value) -> {
                if (!CollectionUtils.isEmpty(value)) {
                    builder.append(key).append(":");
                    builder.append(String.join(" ", value)).append(" ");
                }
            });
        }
        return builder.toString().trim();
    }

    private List<CompanyCard> filterAndTransform(List<DatasetDocument> documents, Map<String, List<String>> metadata, int limit) {
        if (documents == null) {
            return Collections.emptyList();
        }
        return documents.stream()
                .filter(doc -> matchesMetadata(doc, metadata))
                .limit(limit)
                .map(this::toCard)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean matchesMetadata(DatasetDocument document, Map<String, List<String>> metadata) {
        if (metadata.isEmpty()) {
            return true;
        }
        Map<String, Object> docMetadata = document.getMetadata();
        if (docMetadata == null) {
            return metadata.isEmpty();
        }
        for (Map.Entry<String, List<String>> entry : metadata.entrySet()) {
            List<String> filterValues = entry.getValue();
            if (CollectionUtils.isEmpty(filterValues)) {
                continue;
            }
            Object value = docMetadata.get(entry.getKey());
            if (value instanceof List<?> listValue) {
                if (listValue.stream().noneMatch(v -> filterValues.contains(Objects.toString(v, "")))) {
                    return false;
                }
            } else {
                if (value == null || !filterValues.contains(Objects.toString(value, ""))) {
                    return false;
                }
            }
        }
        return true;
    }

    private CompanyCard toCard(DatasetDocument doc) {
        Map<String, Object> metadata = Optional.ofNullable(doc.getMetadata()).orElse(Map.of());
        return CompanyCard.builder()
                .id(doc.getId())
                .name(Optional.ofNullable(doc.getTitle()).orElse("Unknown Company"))
                .industry(asStringList(metadata.get("industry")))
                .size(asString(metadata.get("size")))
                .region(asStringList(metadata.get("region")))
                .techKeywords(asStringList(metadata.get("tech")))
                .summary(Optional.ofNullable(doc.getContent()).orElse(""))
                .score(doc.getScore())
                .reason(Optional.ofNullable(metadata.get("reason"))
                        .map(Object::toString)
                        .orElse("Matched filters"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list.stream().map(Object::toString).collect(Collectors.toList());
        }
        if (value instanceof String str) {
            return List.of(str);
        }
        return List.of();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
