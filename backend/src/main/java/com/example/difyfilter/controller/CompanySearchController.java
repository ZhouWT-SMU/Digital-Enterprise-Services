package com.example.difyfilter.controller;

import com.example.difyfilter.model.dto.CompanyDtos.CompanyCard;
import com.example.difyfilter.model.dto.CompanyDtos.CompanySearchRequest;
import com.example.difyfilter.service.CompanySearchService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/companies")
public class CompanySearchController {

    private final CompanySearchService companySearchService;

    public CompanySearchController(CompanySearchService companySearchService) {
        this.companySearchService = companySearchService;
    }

    @GetMapping("/search")
    public Mono<List<CompanyCard>> search(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "industry", required = false) List<String> industry,
            @RequestParam(value = "size", required = false) List<String> size,
            @RequestParam(value = "region", required = false) List<String> region,
            @RequestParam(value = "tech", required = false) List<String> tech,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        CompanySearchRequest request = new CompanySearchRequest(q, industry, size, region, tech, limit);
        return companySearchService.search(request);
    }
}
