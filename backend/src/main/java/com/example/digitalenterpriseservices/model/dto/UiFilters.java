package com.example.digitalenterpriseservices.model.dto;

import java.util.List;
import lombok.Data;

@Data
public class UiFilters {
    private List<String> industry;
    private List<String> size;
    private List<String> region;
    private List<String> tech;
}
