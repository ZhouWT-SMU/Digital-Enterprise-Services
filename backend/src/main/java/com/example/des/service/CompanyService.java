package com.example.des.service;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.des.dify.DifyClient;
import com.example.des.model.Address;
import com.example.des.model.Company;
import com.example.des.model.Contact;
import com.example.des.model.CoreOffering;
import com.example.des.model.IntellectualProperty;

@Service
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Map<String, Company> companies = new ConcurrentHashMap<>();
    private final DifyClient difyClient;

    public CompanyService(DifyClient difyClient) {
        this.difyClient = difyClient;
    }

    public Company createCompany(Company company) {
        companies.put(company.getId(), company);
        Map<String, Object> workflowPayload = buildWorkflowPayload(company);
        Map<String, Object> workflowResponse = difyClient.triggerWorkflow(workflowPayload);
        log.debug("Workflow response: {}", workflowResponse);
        return company;
    }

    public Collection<Company> listCompanies() {
        return companies.values();
    }

    public Map<String, Object> matchCompany(String query) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("context", companies.values().stream()
                .map(this::buildChatContext)
                .collect(Collectors.toList()));
        return difyClient.invokeChatflow(payload);
    }

    private Map<String, Object> buildWorkflowPayload(Company company) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("company_id", company.getId());
        payload.put("name", company.getName());
        payload.put("unified_social_credit_code", company.getUnifiedSocialCreditCode());
        if (company.getEstablishmentDate() != null) {
            payload.put("establishment_date", DATE_FORMATTER.format(company.getEstablishmentDate()));
        }
        payload.put("scale", company.getScale());
        payload.put("industries", company.getIndustries());
        payload.put("company_type", company.getCompanyType());
        payload.put("business_overview", company.getBusinessOverview());
        payload.put("technology_stack", company.getTechnologyStack());

        Contact contact = company.getContact();
        if (contact != null) {
            Map<String, Object> contactMap = new HashMap<>();
            contactMap.put("name", contact.getName());
            contactMap.put("title", contact.getTitle());
            contactMap.put("phone", contact.getPhone());
            contactMap.put("work_email", contact.getWorkEmail());
            payload.put("contact", contactMap);
        }

        Address address = company.getAddress();
        if (address != null) {
            Map<String, Object> addressMap = new HashMap<>();
            addressMap.put("country", address.getCountry());
            addressMap.put("province", address.getProvince());
            addressMap.put("city", address.getCity());
            addressMap.put("district", address.getDistrict());
            addressMap.put("street", address.getStreetAddress());
            payload.put("address", addressMap);
        }

        payload.put("core_offerings", safeList(company.getCoreOfferings()).stream()
                .map(this::convertOffering)
                .collect(Collectors.toList()));

        payload.put("intellectual_properties", safeList(company.getIntellectualProperties()).stream()
                .map(this::convertIntellectualProperty)
                .collect(Collectors.toList()));

        payload.put("business_license_file_id", company.getBusinessLicenseFileId());
        payload.put("attachments", safeList(company.getAttachmentFileIds()));
        return payload;
    }

    private Map<String, Object> buildChatContext(Company company) {
        Map<String, Object> context = new HashMap<>();
        context.put("company_id", company.getId());
        context.put("name", company.getName());
        context.put("industries", company.getIndustries());
        context.put("business_overview", company.getBusinessOverview());
        context.put("core_offerings", safeList(company.getCoreOfferings()).stream()
                .map(offering -> offering.getName() + "-" + offering.getType())
                .collect(Collectors.toList()));
        return context;
    }

    private Map<String, Object> convertOffering(CoreOffering offering) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", offering.getName());
        map.put("type", offering.getType());
        map.put("description", offering.getDescription());
        return map;
    }

    private Map<String, Object> convertIntellectualProperty(IntellectualProperty property) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", property.getType());
        map.put("registration_number", property.getRegistrationNumber());
        map.put("description", property.getDescription());
        return map;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }
}
