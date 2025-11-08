package com.example.des.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.des.model.DocumentMetadata;

@Service
public class DocumentStorageService {

    private final Map<String, DocumentMetadata> documents = new ConcurrentHashMap<>();

    public DocumentMetadata store(MultipartFile file) throws IOException {
        String id = UUID.randomUUID().toString();
        DocumentMetadata metadata = new DocumentMetadata(id, file.getOriginalFilename(), file.getContentType(), file.getSize(), Instant.now());
        documents.put(id, metadata);
        // 文件内容可以拓展为存储在对象存储或本地文件系统。
        return metadata;
    }

    public Optional<DocumentMetadata> findById(String id) {
        return Optional.ofNullable(documents.get(id));
    }
}
