package com.example.digitalenterpriseservices.client.dify;

import com.example.digitalenterpriseservices.config.AppProperties;
import com.example.digitalenterpriseservices.model.DatasetDocument;
import com.example.digitalenterpriseservices.model.dto.DatasetQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class DifyDatasetClient {

    private final WebClient generalWebClient;
    private final AppProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public Mono<List<DatasetDocument>> search(DatasetQuery query) {
        return generalWebClient.post()
                .uri("/v1/datasets/" + properties.getDatasetId() + "/documents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getDatasetApiKey())
                .bodyValue(buildPayload(query))
                .retrieve()
                .bodyToMono(String.class)
                .map(this::mapToDocuments)
                .onErrorResume(err -> {
                    log.error("Dataset search error", err);
                    return Mono.just(List.of());
                });
    }

    private Map<String, Object> buildPayload(DatasetQuery query) {
        return Map.of(
                "query", query.getQuery(),
                "limit", query.getLimit(),
                "metadata", query.getMetadata()
        );
    }

    private List<DatasetDocument> mapToDocuments(String raw) {
        List<DatasetDocument> documents = new ArrayList<>();
        try {
            JsonNode node = mapper.readTree(raw);
            JsonNode data = node.path("data");
            if (data.isArray()) {
                for (JsonNode item : data) {
                    documents.add(DatasetDocument.builder()
                            .id(item.path("id").asText())
                            .title(item.path("title").asText())
                            .content(item.path("content").asText())
                            .score(item.path("score").asDouble(0.0))
                            .metadata(mapper.convertValue(item.path("metadata"), Map.class))
                            .tags(mapper.convertValue(item.path("tags"), List.class))
                            .build());
                }
            }
        } catch (IOException e) {
            log.error("Failed to parse dataset search response", e);
        }
        return documents;
    }
}
