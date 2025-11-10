package com.example.digitalenterpriseservices.model.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatasetQuery {
    private String query;
    private int limit;
    private Map<String, List<String>> metadata;
}
