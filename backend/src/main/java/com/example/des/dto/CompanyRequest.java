package com.example.des.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CompanyRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^[0-9A-Z]{18}$", message = "统一社会信用代码必须为18位大写字母或数字")
    private String unifiedSocialCreditCode;

    @NotNull
    private LocalDate establishmentDate;

    @NotNull
    @Valid
    private AddressDto address;

    @NotBlank
    private String scale;

    @NotNull
    @Size(min = 1)
    private List<@NotBlank String> industries;

    @NotBlank
    private String companyType;

    @NotBlank
    @Size(min = 500, max = 1500)
    private String businessOverview;

    @NotNull
    @Size(min = 1)
    private List<@Valid CoreOfferingDto> coreOfferings;

    private List<@NotBlank String> technologyStack;

    private List<@Valid IntellectualPropertyDto> intellectualProperties;

    @NotNull
    @Valid
    private ContactDto contact;

    private String businessLicenseFileId;

    private List<String> attachmentFileIds;

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

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
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

    public List<CoreOfferingDto> getCoreOfferings() {
        return coreOfferings;
    }

    public void setCoreOfferings(List<CoreOfferingDto> coreOfferings) {
        this.coreOfferings = coreOfferings;
    }

    public List<String> getTechnologyStack() {
        return technologyStack;
    }

    public void setTechnologyStack(List<String> technologyStack) {
        this.technologyStack = technologyStack;
    }

    public List<IntellectualPropertyDto> getIntellectualProperties() {
        return intellectualProperties;
    }

    public void setIntellectualProperties(List<IntellectualPropertyDto> intellectualProperties) {
        this.intellectualProperties = intellectualProperties;
    }

    public ContactDto getContact() {
        return contact;
    }

    public void setContact(ContactDto contact) {
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
