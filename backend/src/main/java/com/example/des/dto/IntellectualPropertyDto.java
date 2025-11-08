package com.example.des.dto;

import jakarta.validation.constraints.NotBlank;

public class IntellectualPropertyDto {

    @NotBlank
    private String type;

    @NotBlank
    private String registrationNumber;

    private String description;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
