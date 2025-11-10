package com.example.digitalenterpriseservices.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private final Map<String, String> sessionConversationMap = new ConcurrentHashMap<>();

    public String getOrCreateConversation(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        return sessionConversationMap.computeIfAbsent(sessionId, key -> UUID.randomUUID().toString());
    }

    public Optional<String> getConversation(String sessionId) {
        return Optional.ofNullable(sessionConversationMap.get(sessionId));
    }

    public void setConversation(String sessionId, String conversationId) {
        sessionConversationMap.put(sessionId, conversationId);
    }
}
