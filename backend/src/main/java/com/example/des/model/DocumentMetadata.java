package com.example.des.model;

import java.time.Instant;

public class DocumentMetadata {

    private final String id;
    private final String filename;
    private final String contentType;
    private final long size;
    private final Instant uploadedAt;

    public DocumentMetadata(String id, String filename, String contentType, long size, Instant uploadedAt) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.uploadedAt = uploadedAt;
    }

    public String getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
