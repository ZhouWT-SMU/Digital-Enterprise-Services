package com.example.difyfilter.config;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    private final Environment environment;
    private final String difyBaseUrl;

    public WebClientConfig(Environment environment, @Value("${dify.base-url:}") String difyBaseUrl) {
        this.environment = environment;
        this.difyBaseUrl = difyBaseUrl;
    }

    @PostConstruct
    public void ensureEnvironmentVariablesPresent() {
        List<String> required = List.of(
                "DIFY_BASE_URL",
                "DIFY_CHAT_APP_KEY",
                "DIFY_DATASET_API_KEY",
                "DIFY_DATASET_ID"
        );
        Map<String, String> missing = required.stream()
                .filter(name -> environment.getProperty(name) == null || environment.getProperty(name).isBlank())
                .collect(Collectors.toMap(name -> name, name -> ""));
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required environment variables: " + String.join(", ", missing.keySet()));
        }
    }

    @Bean
    public WebClient difyWebClient(WebClient.Builder builder) {
        String base = environment.getProperty("DIFY_BASE_URL", difyBaseUrl);
        if (base == null || base.isBlank()) {
            throw new IllegalStateException("DIFY_BASE_URL must be provided");
        }
        return builder
                .clone()
                .baseUrl(base)
                .defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(120))))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                        .build())
                .build();
    }
}
