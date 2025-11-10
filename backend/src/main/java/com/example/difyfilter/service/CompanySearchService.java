package com.example.difyfilter.service;

import com.example.difyfilter.model.dto.CompanyDtos.CompanyCard;
import com.example.difyfilter.model.dto.CompanyDtos.CompanySearchRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CompanySearchService {

    private final WebClient webClient;
    private final String datasetApiKey;
    private final String datasetId;

    public CompanySearchService(WebClient difyWebClient,
                                @Value("${DIFY_DATASET_API_KEY}") String datasetApiKey,
                                @Value("${DIFY_DATASET_ID}") String datasetId) {
        this.webClient = difyWebClient;
        this.datasetApiKey = datasetApiKey;
        this.datasetId = datasetId;
    }

    public Mono<List<CompanyCard>> search(CompanySearchRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", buildQuery(request));
        payload.put("top_k", request.resolveLimit(20));
        payload.put("rerank", Boolean.TRUE);

        return webClient.post()
                .uri("/v1/datasets/{datasetId}/documents/search", datasetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + datasetApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> mapCards(json, request))
                .map(cards -> filterByMetadata(cards, request))
                .onErrorReturn(Collections.emptyList());
    }

    private String buildQuery(CompanySearchRequest request) {
        List<String> tokens = new ArrayList<>();
        if (request.q() != null && !request.q().isBlank()) {
            tokens.add(request.q().trim());
        }
        appendTokens(tokens, "industry", request.industry());
        appendTokens(tokens, "size", request.size());
        appendTokens(tokens, "region", request.region());
        appendTokens(tokens, "tech", request.tech());
        return tokens.isEmpty() ? "" : String.join(" ", tokens);
    }

    private void appendTokens(List<String> tokens, String label, List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return;
        }
        tokens.add(label + ":" + String.join("|", values));
    }

    private List<CompanyCard> mapCards(JsonNode root, CompanySearchRequest request) {
        List<CompanyCard> cards = new ArrayList<>();
        JsonNode dataNode = root.path("data");
        if (dataNode.isMissingNode()) {
            dataNode = root.path("documents");
        }
        if (dataNode.isArray()) {
            for (JsonNode node : dataNode) {
                String id = node.path("document_id").asText(node.path("id").asText(UUID.randomUUID().toString()));
                String name = node.path("metadata").path("name").asText(node.path("title").asText("未知企业"));
                List<String> industry = toList(node.path("metadata").path("industry"));
                String size = node.path("metadata").path("size").asText(null);
                List<String> region = toList(node.path("metadata").path("region"));
                List<String> tech = toList(node.path("metadata").path("techKeywords"));
                String summary = node.path("content").asText(node.path("metadata").path("summary").asText(""));
                double score = node.path("score").asDouble(0.0);
                String reason = extractReason(node, request);
                cards.add(new CompanyCard(id, name, industry, size, region, tech, summary, score, reason));
            }
        }
        return cards;
    }

    private String extractReason(JsonNode node, CompanySearchRequest request) {
        JsonNode metadata = node.path("metadata");
        List<String> matched = new ArrayList<>();
        addMatchIfPresent(matched, "industry", metadata.path("industry"), request.industry());
        addMatchIfPresent(matched, "size", metadata.path("size"), request.size());
        addMatchIfPresent(matched, "region", metadata.path("region"), request.region());
        addMatchIfPresent(matched, "tech", metadata.path("techKeywords"), request.tech());
        if (!matched.isEmpty()) {
            return "命中：" + String.join("，", matched);
        }
        String snippet = node.path("content").asText();
        if (snippet != null && snippet.length() > 140) {
            snippet = snippet.substring(0, 140) + "...";
        }
        return snippet;
    }

    private void addMatchIfPresent(List<String> matched, String label, JsonNode node, List<String> filters) {
        if (CollectionUtils.isEmpty(filters) || node.isMissingNode()) {
            return;
        }
        Set<String> metadataValues = new HashSet<>(toList(node).stream().map(v -> v.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()));
        for (String value : filters) {
            if (metadataValues.contains(value.toLowerCase(Locale.ROOT))) {
                matched.add(label + "=" + value);
            }
        }
    }

    private List<CompanyCard> filterByMetadata(List<CompanyCard> cards, CompanySearchRequest request) {
        return cards.stream()
                .filter(card -> matches(card.safeIndustry(), request.industry()))
                .filter(card -> request.size() == null || request.size().isEmpty() || request.size().contains(card.size()))
                .filter(card -> matches(card.safeRegion(), request.region()))
                .filter(card -> matches(card.safeTechKeywords(), request.tech()))
                .limit(request.resolveLimit(20))
                .collect(Collectors.toList());
    }

    private boolean matches(List<String> source, List<String> target) {
        if (CollectionUtils.isEmpty(target)) {
            return true;
        }
        if (source == null) {
            return false;
        }
        Set<String> lowered = source.stream().map(v -> v.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        for (String value : target) {
            if (lowered.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private List<String> toList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Collections.emptyList();
        }
        if (node.isArray()) {
            List<String> list = new ArrayList<>();
            for (JsonNode child : node) {
                list.add(child.asText());
            }
            return list;
        }
        return Collections.singletonList(node.asText());
    }
}
