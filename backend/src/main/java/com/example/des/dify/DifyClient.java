package com.example.des.dify;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.des.config.DifyProperties;

@Component
public class DifyClient {

    private static final Logger log = LoggerFactory.getLogger(DifyClient.class);

    private final RestClient restClient;
    private final DifyProperties properties;

    public DifyClient(DifyProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Map<String, Object> triggerWorkflow(Map<String, Object> payload) {
        log.info("Triggering Dify workflow {} with payload: {}", properties.getWorkflowCode(), payload.keySet());
        return restClient.post()
                .uri("/workflows/{code}/execute", properties.getWorkflowCode())
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> invokeChatflow(Map<String, Object> payload) {
        log.info("Invoking Dify chatflow {} with payload: {}", properties.getChatflowCode(), payload.keySet());
        return restClient.post()
                .uri("/chatflows/{code}/messages", properties.getChatflowCode())
                .body(payload)
                .retrieve()
                .body(Map.class);
    }
}
