package com.example.digitalenterpriseservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DigitalEnterpriseServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalEnterpriseServicesApplication.class, args);
    }
}
