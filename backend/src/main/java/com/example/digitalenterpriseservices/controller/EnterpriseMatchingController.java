package com.example.digitalenterpriseservices.controller;

import com.example.digitalenterpriseservices.dto.MatchingRequest;
import com.example.digitalenterpriseservices.dto.MatchingResponse;
import com.example.digitalenterpriseservices.service.EnterpriseMatchingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matching")
@CrossOrigin(origins = "*")
public class EnterpriseMatchingController {

    private final EnterpriseMatchingService matchingService;

    public EnterpriseMatchingController(EnterpriseMatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping
    public ResponseEntity<MatchingResponse> match(@Valid @RequestBody MatchingRequest request) {
        return ResponseEntity.ok(matchingService.match(request));
    }
}
