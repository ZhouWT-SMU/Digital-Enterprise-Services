package com.example.des.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CoreOfferingDto {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "(产品|服务|方案)", message = "类型必须是产品/服务/方案")
    private String type;

    @NotBlank
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
