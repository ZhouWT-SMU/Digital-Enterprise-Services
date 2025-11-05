package com.example.digitalenterpriseservices.service;

import com.example.digitalenterpriseservices.config.DifyWorkflowProperties;
import com.example.digitalenterpriseservices.dto.MatchingRequest;
import com.example.digitalenterpriseservices.dto.MatchingResponse;
import com.example.digitalenterpriseservices.dto.MatchingResponse.SuggestedEnterprise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the Dify workflow trigger endpoint. Once the official
 * <a href="https://github.com/imfangs/dify-java-client">dify-java-client</a> is
 * published to Maven Central, this implementation can be replaced with the
 * native client. For now we keep the HTTP interaction minimal and predictable.
 */
@Component
public class DifyWorkflowClient implements WorkflowClient {

    private static final Logger log = LoggerFactory.getLogger(DifyWorkflowClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final DifyWorkflowProperties properties;

    public DifyWorkflowClient(DifyWorkflowProperties properties) {
        this.properties = properties;
    }

    @Override
    public MatchingResponse matchEnterprises(MatchingRequest request) {
        if (properties.getBaseUrl() == null || properties.getApiKey() == null || properties.getWorkflowId() == null) {
            log.warn("Dify workflow properties not fully configured. Returning placeholder result.");
            return placeholderResponse();
        }

        String url = properties.getBaseUrl() + "/v1/workflows/" + properties.getWorkflowId() + "/trigger";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        Map<String, Object> payload = Map.of(
                "inputs", Map.of(
                        "requirements", request.requirements(),
                        "regions", request.regions(),
                        "companyTypes", request.companyTypes(),
                        "budgets", request.budgets(),
                        "coreTechnologies", request.coreTechnologies(),
                        "products", request.products()
                ),
                "response_mode", "blocking"
        );

        try {
            // The exact response schema depends on the Dify workflow configuration.
            // Here we simply forward the raw payload into our domain response object.
            Map<String, Object> response = restTemplate.postForObject(url, new HttpEntity<>(payload, headers), Map.class);
            return new MatchingResponse(
                    "Dify 工作流响应 (待解析)",
                    List.of(new SuggestedEnterprise(
                            "占位企业",
                            response != null ? response.toString() : "工作流未返回内容",
                            "N/A",
                            "N/A"
                    ))
            );
        } catch (RestClientException ex) {
            log.error("调用 Dify 工作流失败，返回占位结果", ex);
            return placeholderResponse();
        }
    }

    private MatchingResponse placeholderResponse() {
        return new MatchingResponse(
                "工作流暂未集成，以下为模拟匹配结果",
                List.of(
                        new SuggestedEnterprise(
                                "示例科技有限公司",
                                "专注于工业互联网解决方案，可根据您的需求定制服务。",
                                "0.82",
                                "contact@example.com"
                        ),
                        new SuggestedEnterprise(
                                "未来智造股份",
                                "提供智能制造全链路服务，涵盖方案咨询与系统部署。",
                                "0.76",
                                "service@example.com"
                        )
                )
        );
    }
}
