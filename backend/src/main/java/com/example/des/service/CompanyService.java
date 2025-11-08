package com.example.des.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.des.dify.DifyClient;
import com.example.des.dto.CompanyRequest;
import com.example.des.model.Company;
import com.example.des.repository.CompanyRepository;

@Service
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository repository;
    private final CompanyMapper mapper;
    private final DifyClient difyClient;

    public CompanyService(CompanyRepository repository, CompanyMapper mapper, DifyClient difyClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.difyClient = difyClient;
    }

    public Company createCompany(CompanyRequest request) {
        Company company = mapper.fromRequest(request);
        repository.save(company);
        Map<String, Object> workflowPayload = buildWorkflowPayload(company);
        Map<String, Object> workflowResponse = difyClient.triggerWorkflow(workflowPayload);
        log.debug("Workflow response: {}", workflowResponse);
        return company;
    }

    public Collection<Company> listCompanies() {
        return repository.findAll();
    }

    public Map<String, Object> matchCompany(String query) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("context", repository.findAll().stream()
                .map(this::buildChatContext)
                .collect(Collectors.toList()));
        return difyClient.invokeChatflow(payload);
    }

    private Map<String, Object> buildWorkflowPayload(Company company) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("company_id", company.getId());
        payload.put("name", company.getName());
        payload.put("unified_social_credit_code", company.getUnifiedSocialCreditCode());
        payload.put("establishment_date", company.getEstablishmentDate().toString());
        payload.put("scale", company.getScale());
        payload.put("industries", company.getIndustries());
        payload.put("company_type", company.getCompanyType());
        payload.put("business_overview", company.getBusinessOverview());
        payload.put("technology_stack", company.getTechnologyStack());
        payload.put("contact", Map.of(
                "name", company.getContact().getName(),
                "title", company.getContact().getTitle(),
                "phone", company.getContact().getPhone(),
                "work_email", company.getContact().getWorkEmail()));
        payload.put("address", Map.of(
                "country", company.getAddress().getCountry(),
                "province", company.getAddress().getProvince(),
                "city", company.getAddress().getCity(),
                "district", company.getAddress().getDistrict(),
                "street", company.getAddress().getStreetAddress()));
        payload.put("core_offerings", company.getCoreOfferings().stream()
                .map(offering -> Map.of(
                        "name", offering.getName(),
                        "type", offering.getType(),
                        "description", offering.getDescription()))
                .collect(Collectors.toList()));
        payload.put("intellectual_properties", company.getIntellectualProperties().stream()
                .map(ip -> Map.of(
                        "type", ip.getType(),
                        "registration_number", ip.getRegistrationNumber(),
                        "description", ip.getDescription()))
                .collect(Collectors.toList()));
        payload.put("business_license_file_id", company.getBusinessLicenseFileId());
        payload.put("attachments", company.getAttachmentFileIds());
        return payload;
    }

    private Map<String, Object> buildChatContext(Company company) {
        Map<String, Object> context = new HashMap<>();
        context.put("company_id", company.getId());
        context.put("name", company.getName());
        context.put("industries", company.getIndustries());
        context.put("business_overview", company.getBusinessOverview());
        context.put("core_offerings", company.getCoreOfferings().stream()
                .map(offering -> offering.getName() + "-" + offering.getType())
                .collect(Collectors.toList()));
        return context;
    }
}
