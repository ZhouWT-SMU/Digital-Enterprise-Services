package com.example.des.model;

public class IntellectualProperty {

    private String type;
    private String registrationNumber;
    private String description;

    public IntellectualProperty() {
    }

    public IntellectualProperty(String type, String registrationNumber, String description) {
        this.type = type;
        this.registrationNumber = registrationNumber;
        this.description = description;
    }

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
