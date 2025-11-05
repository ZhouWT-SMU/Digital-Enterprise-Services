package com.example.digitalenterpriseservices.service;

import com.example.digitalenterpriseservices.dto.MatchingRequest;
import com.example.digitalenterpriseservices.dto.MatchingResponse;
import org.springframework.stereotype.Service;

@Service
public class EnterpriseMatchingService {

    private final WorkflowClient workflowClient;

    public EnterpriseMatchingService(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    public MatchingResponse match(MatchingRequest request) {
        return workflowClient.matchEnterprises(request);
    }
}
