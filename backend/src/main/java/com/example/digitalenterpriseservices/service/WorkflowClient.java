package com.example.digitalenterpriseservices.service;

import com.example.digitalenterpriseservices.dto.MatchingRequest;
import com.example.digitalenterpriseservices.dto.MatchingResponse;

public interface WorkflowClient {

    MatchingResponse matchEnterprises(MatchingRequest request);
}
