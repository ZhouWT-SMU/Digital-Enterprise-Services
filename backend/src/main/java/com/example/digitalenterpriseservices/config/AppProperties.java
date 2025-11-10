package com.example.digitalenterpriseservices.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "dify")
public class AppProperties {
    private String baseUrl;
    private String chatAppKey;
    private String datasetApiKey;
    private String datasetId;
}
