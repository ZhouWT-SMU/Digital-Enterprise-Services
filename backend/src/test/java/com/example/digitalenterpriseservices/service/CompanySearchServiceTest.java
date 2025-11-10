package com.example.digitalenterpriseservices.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.digitalenterpriseservices.client.dify.DifyDatasetClient;
import com.example.digitalenterpriseservices.model.DatasetDocument;
import com.example.digitalenterpriseservices.model.dto.CompanyCard;
import com.example.digitalenterpriseservices.model.dto.UiFilters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

class CompanySearchServiceTest {

    @Mock
    private DifyDatasetClient datasetClient;

    private CompanySearchService companySearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        companySearchService = new CompanySearchService(datasetClient);
    }

    @Test
    void shouldFilterResultsBasedOnMetadata() {
        DatasetDocument matching = DatasetDocument.builder()
                .id("1")
                .title("ACME Manufacturing")
                .content("AI powered manufacturing")
                .score(0.9)
                .metadata(Map.of(
                        "industry", List.of("制造业"),
                        "size", "中型",
                        "region", List.of("华东"),
                        "tech", List.of("Kubernetes")
                ))
                .build();

        DatasetDocument nonMatching = DatasetDocument.builder()
                .id("2")
                .title("Finance Corp")
                .content("Financial services")
                .score(0.8)
                .metadata(Map.of(
                        "industry", List.of("金融"),
                        "size", "大型"
                ))
                .build();

        when(datasetClient.search(any())).thenReturn(Mono.just(List.of(matching, nonMatching)));

        UiFilters filters = new UiFilters();
        filters.setIndustry(List.of("制造业"));
        filters.setSize(List.of("中型"));
        filters.setRegion(List.of("华东"));
        filters.setTech(List.of("Kubernetes"));

        List<CompanyCard> result = companySearchService.searchCompanies("AI", filters, 20).block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("ACME Manufacturing");
        assertThat(result.get(0).getTechKeywords()).contains("Kubernetes");
    }

    @Test
    void shouldFallbackToQueryAugmentationWhenNoFilters() {
        DatasetDocument doc = DatasetDocument.builder()
                .id("1")
                .title("AI Startup")
                .content("AI startup in Beijing")
                .score(0.7)
                .metadata(Map.of())
                .build();

        when(datasetClient.search(any())).thenAnswer(invocation -> {
            return Mono.just(List.of(doc));
        });

        List<CompanyCard> result = companySearchService.searchCompanies("AI startup", null, null).block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("AI Startup");
    }
}
