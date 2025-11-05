package com.example.digitalenterpriseservices.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/patents")
public class PatentSearchController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> placeholder() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of(
                        "message", "专利搜寻模块尚未实现，请稍后再试。"
                ));
    }
}
