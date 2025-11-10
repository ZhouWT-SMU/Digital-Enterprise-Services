package com.example.digitalenterpriseservices.controller;

import com.example.digitalenterpriseservices.model.dto.CompanyCard;
import com.example.digitalenterpriseservices.model.dto.UiFilters;
import com.example.digitalenterpriseservices.service.CompanySearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanySearchController {

    private final CompanySearchService companySearchService;

    @GetMapping("/search")
    public Mono<List<CompanyCard>> search(@RequestParam(value = "q", required = false) String query,
                                          @RequestParam(value = "industry", required = false) List<String> industry,
                                          @RequestParam(value = "size", required = false) List<String> size,
                                          @RequestParam(value = "region", required = false) List<String> region,
                                          @RequestParam(value = "tech", required = false) List<String> tech,
                                          @RequestParam(value = "limit", required = false) Integer limit) {
        UiFilters filters = new UiFilters();
        filters.setIndustry(industry);
        filters.setSize(size);
        filters.setRegion(region);
        filters.setTech(tech);
        return companySearchService.searchCompanies(query, filters, limit);
    }
}
