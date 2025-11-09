package com.example.des.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dify")
public class DifyProperties {

    private String apiKey;

    private String baseUrl;

    private String workflowCode;

    private String chatflowCode;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getWorkflowCode() {
        return workflowCode;
    }

    public void setWorkflowCode(String workflowCode) {
        this.workflowCode = workflowCode;
    }

    public String getChatflowCode() {
        return chatflowCode;
    }

    public void setChatflowCode(String chatflowCode) {
        this.chatflowCode = chatflowCode;
    }
}
