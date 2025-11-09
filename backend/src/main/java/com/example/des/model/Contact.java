package com.example.des.model;

public class Contact {

    private String name;
    private String title;
    private String phone;
    private String workEmail;

    public Contact() {
    }

    public Contact(String name, String title, String phone, String workEmail) {
        this.name = name;
        this.title = title;
        this.phone = phone;
        this.workEmail = workEmail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWorkEmail() {
        return workEmail;
    }

    public void setWorkEmail(String workEmail) {
        this.workEmail = workEmail;
    }
}
