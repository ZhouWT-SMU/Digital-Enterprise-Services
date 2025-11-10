package com.example.digitalenterpriseservices;

import com.example.digitalenterpriseservices.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class DigitalEnterpriseServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalEnterpriseServicesApplication.class, args);
    }
}
