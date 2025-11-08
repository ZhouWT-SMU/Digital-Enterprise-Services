package com.example.des.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Company {

    private final String id;
    private final Instant createdAt;

    private String name;
    private String unifiedSocialCreditCode;
    private LocalDate establishmentDate;
    private Address address;
    private String scale;
    private List<String> industries = new ArrayList<>();
    private String companyType;
    private String businessOverview;
    private List<CoreOffering> coreOfferings = new ArrayList<>();
    private List<String> technologyStack = new ArrayList<>();
    private List<IntellectualProperty> intellectualProperties = new ArrayList<>();
    private Contact contact;
    private String businessLicenseFileId;
    private List<String> attachmentFileIds = new ArrayList<>();

    public Company() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnifiedSocialCreditCode() {
        return unifiedSocialCreditCode;
    }

    public void setUnifiedSocialCreditCode(String unifiedSocialCreditCode) {
        this.unifiedSocialCreditCode = unifiedSocialCreditCode;
    }

    public LocalDate getEstablishmentDate() {
        return establishmentDate;
    }

    public void setEstablishmentDate(LocalDate establishmentDate) {
        this.establishmentDate = establishmentDate;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    public List<String> getIndustries() {
        return industries;
    }

    public void setIndustries(List<String> industries) {
        this.industries = industries;
    }

    public String getCompanyType() {
        return companyType;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    public String getBusinessOverview() {
        return businessOverview;
    }

    public void setBusinessOverview(String businessOverview) {
        this.businessOverview = businessOverview;
    }

    public List<CoreOffering> getCoreOfferings() {
        return coreOfferings;
    }

    public void setCoreOfferings(List<CoreOffering> coreOfferings) {
        this.coreOfferings = coreOfferings;
    }

    public List<String> getTechnologyStack() {
        return technologyStack;
    }

    public void setTechnologyStack(List<String> technologyStack) {
        this.technologyStack = technologyStack;
    }

    public List<IntellectualProperty> getIntellectualProperties() {
        return intellectualProperties;
    }

    public void setIntellectualProperties(List<IntellectualProperty> intellectualProperties) {
        this.intellectualProperties = intellectualProperties;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public String getBusinessLicenseFileId() {
        return businessLicenseFileId;
    }

    public void setBusinessLicenseFileId(String businessLicenseFileId) {
        this.businessLicenseFileId = businessLicenseFileId;
    }

    public List<String> getAttachmentFileIds() {
        return attachmentFileIds;
    }

    public void setAttachmentFileIds(List<String> attachmentFileIds) {
        this.attachmentFileIds = attachmentFileIds;
    }
}
