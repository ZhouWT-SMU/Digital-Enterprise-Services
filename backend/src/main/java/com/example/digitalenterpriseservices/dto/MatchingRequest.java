package com.example.digitalenterpriseservices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MatchingRequest(
        @NotBlank(message = "需求描述不能为空")
        @Size(max = 2000, message = "需求描述过长")
        String requirements,

        List<String> regions,
        List<String> companyTypes,
        List<String> budgets,
        List<String> coreTechnologies,
        List<String> products
) {
}
