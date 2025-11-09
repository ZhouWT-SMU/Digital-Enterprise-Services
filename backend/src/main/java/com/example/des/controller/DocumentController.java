package com.example.des.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.des.model.DocumentMetadata;
import com.example.des.service.DocumentStorageService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentStorageService storageService;

    public DocumentController(DocumentStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<DocumentMetadata> upload(@RequestParam("file") MultipartFile file) throws IOException {
        DocumentMetadata metadata = storageService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(metadata);
    }
}
