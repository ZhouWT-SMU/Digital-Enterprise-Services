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
        String context = companies.values().stream()
                .map(this::buildChatContext)
                .collect(Collectors.joining("\n\n---\n\n"));
        payload.put("context", context);
        return difyClient.invokeChatflow(payload);
    }

    private Map<String, Object> buildWorkflowPayload(Company company) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("company_id_text", company.getId());
        payload.put("company_name_text", company.getName());
        payload.put("credit_code_text", company.getUnifiedSocialCreditCode());
        if (company.getEstablishmentDate() != null) {
            payload.put("establish_date_text", DATE_FORMATTER.format(company.getEstablishmentDate()));
        }
        payload.put("scale_dropdown", company.getScale());
        payload.put("industry_paragraph", String.join(", ", safeList(company.getIndustries())));
        payload.put("company_type_dropdown", company.getCompanyType());
        payload.put("business_overview_paragraph", company.getBusinessOverview());
        payload.put("core_offerings_paragraph", formatOfferings(company.getCoreOfferings()));
        payload.put("technology_stack_text", String.join(", ", safeList(company.getTechnologyStack())));

        List<IntellectualProperty> intellectualProperties = safeList(company.getIntellectualProperties());
        payload.put("has_intellectual_property_checkbox", !intellectualProperties.isEmpty());
        payload.put("intellectual_property_paragraph", formatIntellectualProperties(intellectualProperties));

        Contact contact = company.getContact();
        if (contact != null) {
            payload.put("contact_name_text", contact.getName());
            payload.put("contact_title_text", contact.getTitle());
            payload.put("contact_phone_text", contact.getPhone());
            payload.put("contact_email_text", contact.getWorkEmail());
        }

        Address address = company.getAddress();
        if (address != null) {
            payload.put("address_paragraph", formatAddress(address));
        }

        payload.put("business_license_file", company.getBusinessLicenseFileId());
        payload.put("attachment_file_list", safeList(company.getAttachmentFileIds()));
        return payload;
    }

    private String buildChatContext(Company company) {
        StringBuilder builder = new StringBuilder();
        builder.append("企业ID：").append(nullToEmpty(company.getId())).append('\n');
        builder.append("企业名称：").append(nullToEmpty(company.getName())).append('\n');
        builder.append("行业：").append(String.join(", ", safeList(company.getIndustries()))).append('\n');
        builder.append("规模：").append(nullToEmpty(company.getScale())).append('\n');
        builder.append("业务简介：").append(nullToEmpty(company.getBusinessOverview())).append('\n');
        builder.append("核心产品/服务：").append(formatOfferings(company.getCoreOfferings())).append('\n');
        builder.append("技术栈：").append(String.join(", ", safeList(company.getTechnologyStack())));
        return builder.toString();
    }

    private String formatOfferings(List<CoreOffering> offerings) {
        List<CoreOffering> safeOfferings = safeList(offerings);
        if (safeOfferings.isEmpty()) {
            return "暂无信息";
        }
        return safeOfferings.stream()
                .map(offering -> String.format("%s（%s）：%s",
                        nullToEmpty(offering.getName()),
                        nullToEmpty(offering.getType()),
                        nullToEmpty(offering.getDescription())))
                .collect(Collectors.joining("\n"));
    }

    private String formatIntellectualProperties(List<IntellectualProperty> properties) {
        if (properties.isEmpty()) {
            return "未提供知识产权信息";
        }
        return properties.stream()
                .map(property -> String.format("%s：%s（%s）",
                        nullToEmpty(property.getType()),
                        nullToEmpty(property.getRegistrationNumber()),
                        nullToEmpty(property.getDescription())))
                .collect(Collectors.joining("\n"));
    }

    private String formatAddress(Address address) {
        return String.join(" ", List.of(
                nullToEmpty(address.getCountry()),
                nullToEmpty(address.getProvince()),
                nullToEmpty(address.getCity()),
                nullToEmpty(address.getDistrict()),
                nullToEmpty(address.getStreetAddress()))).trim();
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
