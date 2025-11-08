package com.example.des.model;

public class Address {

    private String country;
    private String province;
    private String city;
    private String district;
    private String streetAddress;

    public Address() {
    }

    public Address(String country, String province, String city, String district, String streetAddress) {
        this.country = country;
        this.province = province;
        this.city = city;
        this.district = district;
        this.streetAddress = streetAddress;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }
}
