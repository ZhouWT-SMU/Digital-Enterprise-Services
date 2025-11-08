package com.example.des.service;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.des.dto.AddressDto;
import com.example.des.dto.CompanyRequest;
import com.example.des.dto.ContactDto;
import com.example.des.dto.CoreOfferingDto;
import com.example.des.dto.IntellectualPropertyDto;
import com.example.des.model.Address;
import com.example.des.model.Company;
import com.example.des.model.Contact;
import com.example.des.model.CoreOffering;
import com.example.des.model.IntellectualProperty;

@Component
public class CompanyMapper {

    public Company fromRequest(CompanyRequest request) {
        Company company = new Company();
        company.setName(request.getName());
        company.setUnifiedSocialCreditCode(request.getUnifiedSocialCreditCode());
        company.setEstablishmentDate(request.getEstablishmentDate());
        company.setAddress(mapAddress(request.getAddress()));
        company.setScale(request.getScale());
        company.setIndustries(request.getIndustries());
        company.setCompanyType(request.getCompanyType());
        company.setBusinessOverview(request.getBusinessOverview());
        company.setCoreOfferings(request.getCoreOfferings().stream().map(this::mapCoreOffering).collect(Collectors.toList()));
        if (request.getTechnologyStack() != null) {
            company.setTechnologyStack(request.getTechnologyStack());
        }
        if (request.getIntellectualProperties() != null) {
            company.setIntellectualProperties(request.getIntellectualProperties().stream()
                    .map(this::mapIntellectualProperty)
                    .collect(Collectors.toList()));
        }
        company.setContact(mapContact(request.getContact()));
        company.setBusinessLicenseFileId(request.getBusinessLicenseFileId());
        if (request.getAttachmentFileIds() != null) {
            company.setAttachmentFileIds(request.getAttachmentFileIds());
        }
        return company;
    }

    private Address mapAddress(AddressDto dto) {
        return new Address(dto.getCountry(), dto.getProvince(), dto.getCity(), dto.getDistrict(), dto.getStreetAddress());
    }

    private Contact mapContact(ContactDto dto) {
        return new Contact(dto.getName(), dto.getTitle(), dto.getPhone(), dto.getWorkEmail());
    }

    private CoreOffering mapCoreOffering(CoreOfferingDto dto) {
        return new CoreOffering(dto.getName(), dto.getType(), dto.getDescription());
    }

    private IntellectualProperty mapIntellectualProperty(IntellectualPropertyDto dto) {
        return new IntellectualProperty(dto.getType(), dto.getRegistrationNumber(), dto.getDescription());
    }
}
