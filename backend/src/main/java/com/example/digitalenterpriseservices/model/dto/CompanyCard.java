package com.example.digitalenterpriseservices.model.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyCard {
    private String id;
    private String name;
    private List<String> industry;
    private String size;
    private List<String> region;
    private List<String> techKeywords;
    private String summary;
    private double score;
    private String reason;
}
