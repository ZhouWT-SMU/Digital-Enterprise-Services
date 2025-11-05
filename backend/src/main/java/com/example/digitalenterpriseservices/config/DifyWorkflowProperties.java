package com.example.digitalenterpriseservices.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "dify.workflow")
public class DifyWorkflowProperties {

    /**
     * Dify Workflow API endpoint. Example: https://api.dify.ai
     */
    private String baseUrl;

    /**
     * API Key for authenticating requests to Dify.
     */
    private String apiKey;

    /**
     * Workflow ID or name to be triggered.
     */
    private String workflowId;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }
}
