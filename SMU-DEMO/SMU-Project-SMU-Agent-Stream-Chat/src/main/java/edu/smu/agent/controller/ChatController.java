package edu.smu.agent.controller;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import edu.smu.agent.service.DifyChatFlowService;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import io.github.imfangs.dify.client.model.chat.Conversation;
import io.github.imfangs.dify.client.model.chat.ConversationListResponse;
import io.github.imfangs.dify.client.model.chat.MessageListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 允许跨域请求
public class ChatController {

    private final DifyChatFlowService difyChatFlowService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("message");
            String userId = request.get("userId");

            if (query == null || userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "消息和用户ID不能为空"));
            }

            ChatMessageResponse response = difyChatFlowService.sendMessage(query, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("messageId", response.getMessageId());
            result.put("conversationId", response.getConversationId());
            result.put("answer", response.getAnswer());
            result.put("mode", response.getMode());
            result.put("createdAt", response.getCreatedAt());

            // 如果有元数据，也返回
            if (response.getMetadata() != null) {
                result.put("metadata", response.getMetadata());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("发送消息失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 发送流式消息
     */
    @PostMapping("/send-stream")
    public ResponseEntity<StreamingResponseBody> sendStreamMessage(@RequestBody Map<String, String> request) {
        String query = request.get("message");
        String userId = request.get("userId");
        String conversationId = request.get("conversationId");

        if (query == null || userId == null) {
            StreamingResponseBody errorBody = outputStream -> {
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                    Map<String, Object> errorPayload = Map.of(
                            "type", "error",
                            "error", "消息和用户ID不能为空");
                    writer.write(objectMapper.writeValueAsString(errorPayload));
                    writer.write("\n");
                    writer.flush();
                }
            };
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.parseMediaType("application/x-ndjson"))
                    .body(errorBody);
        }

        StreamingResponseBody body = outputStream -> {
            final BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            final Object writeLock = new Object();
            final AtomicBoolean streamFailed = new AtomicBoolean(false);

            try {
                ChatMessageResponse response = difyChatFlowService.sendStreamMessage(query, userId, conversationId, chunk -> {
                    if (chunk == null || chunk.isEmpty() || streamFailed.get()) {
                        return;
                    }

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "chunk");
                    payload.put("delta", chunk);
                    payload.put("createdAt", System.currentTimeMillis());

                    try {
                        synchronized (writeLock) {
                            writer.write(objectMapper.writeValueAsString(payload));
                            writer.write("\n");
                            writer.flush();
                        }
                    } catch (Exception ioException) {
                        streamFailed.set(true);
                        throw new RuntimeException(ioException);
                    }
                });

                if (response != null && !streamFailed.get()) {
                    Map<String, Object> finalPayload = new HashMap<>();
                    finalPayload.put("type", "result");
                    finalPayload.put("answer", response.getAnswer());
                    finalPayload.put("conversationId", response.getConversationId());
                    finalPayload.put("messageId", response.getMessageId());
                    finalPayload.put("createdAt", response.getCreatedAt());

                    if (response.getMetadata() != null) {
                        finalPayload.put("metadata", response.getMetadata());
                    }

                    synchronized (writeLock) {
                        writer.write(objectMapper.writeValueAsString(finalPayload));
                        writer.write("\n");
                        writer.flush();
                    }
                }
            } catch (Exception streamError) {
                log.error("发送流式消息失败", streamError);

                if (!streamFailed.get()) {
                    Map<String, Object> errorPayload = new HashMap<>();
                    errorPayload.put("type", "error");
                    errorPayload.put("error", streamError.getMessage());

                    try {
                        synchronized (writeLock) {
                            writer.write(objectMapper.writeValueAsString(errorPayload));
                            writer.write("\n");
                            writer.flush();
                        }
                    } catch (Exception ignored) {
                        // 忽略写入错误，确保连接关闭
                    }
                }
            } finally {
                try {
                    writer.flush();
                } catch (Exception ignored) {
                    // ignore flush errors on close
                }
            }
        };

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .body(body);
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/history/{conversationId}")
    public ResponseEntity<Map<String, Object>> getMessageHistory(
            @PathVariable String conversationId,
            @RequestParam String userId,
            @RequestParam(defaultValue = "20") Integer limit) {
        try {
            MessageListResponse response = difyChatFlowService.getMessageHistory(conversationId, userId, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response.getData());
            result.put("hasMore", response.getHasMore());
            result.put("limit", response.getLimit());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("获取消息历史失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取用户会话列表
     */
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations(
            @RequestParam String userId,
            @RequestParam(defaultValue = "20") Integer limit) {
        try {
            ConversationListResponse response = difyChatFlowService.getConversations(userId, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response.getData());
            result.put("hasMore", response.getHasMore());
            result.put("limit", response.getLimit());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Map<String, Object>> deleteConversation(
            @PathVariable String conversationId,
            @RequestParam String userId) {
        try {
            difyChatFlowService.deleteConversation(conversationId, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "会话删除成功");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("删除会话失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 重命名会话
     */
    @PutMapping("/conversations/{conversationId}/name")
    public ResponseEntity<Map<String, Object>> renameConversation(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String userId = request.get("userId");

            if (name == null || userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "名称和用户ID不能为空"));
            }

            Conversation conversation = difyChatFlowService.renameConversation(conversationId, name, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("conversation", conversation);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("重命名会话失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

}