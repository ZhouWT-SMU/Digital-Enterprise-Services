package com.example.digitalenterpriseservices.model;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatasetDocument {
    private String id;
    private String title;
    private String content;
    private double score;
    private Map<String, Object> metadata;
    private List<String> tags;
}
