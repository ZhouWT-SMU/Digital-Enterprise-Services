package com.example.difyfilter.controller;

import com.example.difyfilter.model.dto.ChatDtos.ChatStreamRequest;
import com.example.difyfilter.service.ChatService;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestParam("message") String message,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "filters", required = false) String filtersJson
    ) {
        Map<String, List<String>> filters = JsonParamUtils.decodeFilters(filtersJson);
        ChatStreamRequest request = new ChatStreamRequest(message, sessionId, null, filters);
        return chatService.stream(request);
    }
}
