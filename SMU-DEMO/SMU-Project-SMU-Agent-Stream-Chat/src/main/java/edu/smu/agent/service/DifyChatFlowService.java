package edu.smu.agent.service;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.DifyClientFactory;
import io.github.imfangs.dify.client.callback.ChatStreamCallback;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.AgentLogEvent;
import io.github.imfangs.dify.client.event.AgentMessageEvent;
import io.github.imfangs.dify.client.event.AgentThoughtEvent;
import io.github.imfangs.dify.client.event.ErrorEvent;
import io.github.imfangs.dify.client.event.MessageEndEvent;
import io.github.imfangs.dify.client.event.MessageEvent;
import io.github.imfangs.dify.client.event.MessageFileEvent;
import io.github.imfangs.dify.client.event.MessageReplaceEvent;
import io.github.imfangs.dify.client.event.PingEvent;
import io.github.imfangs.dify.client.event.TtsMessageEndEvent;
import io.github.imfangs.dify.client.event.TtsMessageEvent;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import io.github.imfangs.dify.client.model.chat.Conversation;
import io.github.imfangs.dify.client.model.chat.ConversationListResponse;
import io.github.imfangs.dify.client.model.chat.MessageListResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Dify 服务类
 * 封装 Dify Chatflow API 调用
 */
@Slf4j
@Service
public class DifyChatFlowService {

    @Value("${dify.base-url}")
    private String baseUrl;

    @Value("${dify.chatflow-api-key}")
    private String chatflowApiKey;

    /**
     * 发送消息（阻塞模式）
     */
    public ChatMessageResponse sendMessage(String query, String userId) {
        try (DifyChatflowClient client = DifyClientFactory.createChatWorkflowClient(baseUrl, chatflowApiKey)) {
            ChatMessage message = ChatMessage.builder()
                    .query(query)
                    .user(userId)
                    .responseMode(ResponseMode.BLOCKING)
                    .build();

            log.info("发送消息: user={}, query={}", userId, query);
            ChatMessageResponse response = client.sendChatMessage(message);
            log.info("收到响应: messageId={}", response.getMessageId());

            return response;
        } catch (Exception e) {
            log.error("发送消息失败: user={}, query={}", userId, query, e);
            throw new RuntimeException("发送消息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送消息（流式模式）
     */
    public ChatMessageResponse sendStreamMessage(String query, String userId) {
        return sendStreamMessage(query, userId, null, null);
    }

    /**
     * 发送消息（流式模式），支持片段回调
     */
    public ChatMessageResponse sendStreamMessage(String query, String userId, String conversationId,
            Consumer<String> chunkConsumer) {
        try (DifyChatflowClient client = DifyClientFactory.createChatWorkflowClient(baseUrl, chatflowApiKey)) {
            ChatMessage.ChatMessageBuilder messageBuilder = ChatMessage.builder()
                    .query(query)
                    .user(userId)
                    .responseMode(ResponseMode.STREAMING);

            if (conversationId != null && !conversationId.isBlank()) {
                messageBuilder.conversationId(conversationId);
            }

            ChatMessage message = messageBuilder.build();

            log.info("发送流式消息: user={}, query={}, conversationId={}", userId, query, conversationId);

            CountDownLatch latch = new CountDownLatch(1);
            StringBuilder responseBuilder = new StringBuilder();
            AtomicReference<String> messageId = new AtomicReference<>();
            AtomicReference<String> conversationRef = new AtomicReference<>(conversationId);
            AtomicReference<Exception> exceptionRef = new AtomicReference<>();
            AtomicBoolean hasContent = new AtomicBoolean(false);
            AtomicBoolean streamTerminated = new AtomicBoolean(false);

            Runnable stopStreaming = () -> {
                if (!streamTerminated.getAndSet(true)) {
                    latch.countDown();
                }
            };

            client.sendChatMessageStream(message, new ChatStreamCallback() {
                @Override
                public void onMessage(MessageEvent event) {
                    if (streamTerminated.get()) {
                        return;
                    }

                    String answer = event.getAnswer();
                    if (answer != null && !answer.isEmpty()) {
                        log.info("收到消息片段: {}", answer);
                        responseBuilder.append(answer);
                        hasContent.set(true);

                        if (chunkConsumer != null) {
                            try {
                                chunkConsumer.accept(answer);
                            } catch (Exception consumerError) {
                                exceptionRef.set(new RuntimeException("处理流式片段失败", consumerError));
                                stopStreaming.run();
                            }
                        }
                    }

                    String eventConversationId = extractString(event, "getConversationId");
                    if (eventConversationId != null && !eventConversationId.isEmpty()) {
                        conversationRef.set(eventConversationId);
                    }
                }

                @Override
                public void onMessageEnd(MessageEndEvent event) {
                    log.info("消息结束，完整消息ID: {}", event.getMessageId());
                    messageId.set(event.getMessageId());

                    String eventConversationId = extractString(event, "getConversationId");
                    if (eventConversationId != null && !eventConversationId.isEmpty()) {
                        conversationRef.set(eventConversationId);
                    }

                    String eventAnswer = extractString(event, "getAnswer");
                    if (eventAnswer != null && !eventAnswer.isEmpty() && responseBuilder.length() == 0) {
                        responseBuilder.append(eventAnswer);
                        hasContent.set(true);

                        if (chunkConsumer != null) {
                            try {
                                chunkConsumer.accept(eventAnswer);
                            } catch (Exception consumerError) {
                                exceptionRef.set(new RuntimeException("处理流式片段失败", consumerError));
                            }
                        }
                    }

                    stopStreaming.run();
                }

                @Override
                public void onMessageFile(MessageFileEvent event) {
                    log.info("收到文件: {}", event);
                }

                @Override
                public void onTTSMessage(TtsMessageEvent event) {
                    log.info("收到TTS消息: {}", event);
                }

                @Override
                public void onTTSMessageEnd(TtsMessageEndEvent event) {
                    log.info("TTS消息结束: {}", event);
                }

                @Override
                public void onMessageReplace(MessageReplaceEvent event) {
                    log.info("消息替换: {}", event);
                }

                @Override
                public void onAgentMessage(AgentMessageEvent event) {
                    log.info("Agent消息: {}", event);
                }

                @Override
                public void onAgentThought(AgentThoughtEvent event) {
                    log.info("Agent思考: {}", event);
                }

                @Override
                public void onAgentLog(AgentLogEvent event) {
                    log.info("Agent日志: {}", event);
                }

                @Override
                public void onError(ErrorEvent event) {
                    log.error("流式响应错误: {}", event.getMessage());
                    exceptionRef.set(new RuntimeException("流式响应错误: " + event.getMessage()));
                    stopStreaming.run();
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("流式响应异常: {}", throwable.getMessage(), throwable);
                    exceptionRef.set(new RuntimeException("流式响应异常: " + throwable.getMessage(), throwable));
                    stopStreaming.run();
                }

                @Override
                public void onPing(PingEvent event) {
                    log.info("心跳: {}", event);
                }
            });

            boolean completed = latch.await(60, TimeUnit.SECONDS);

            Exception exception = exceptionRef.get();
            if (exception != null) {
                throw new RuntimeException("流式响应失败", exception);
            }

            if (!completed && hasContent.get() && responseBuilder.length() > 0) {
                log.warn("未收到 onMessageEnd 事件，但已收集到内容，返回部分响应");

                ChatMessageResponse response = new ChatMessageResponse();
                String fallbackId = "stream-" + System.currentTimeMillis();
                response.setMessageId(messageId.get() != null ? messageId.get() : fallbackId);
                response.setAnswer(responseBuilder.toString());
                response.setConversationId(
                        conversationRef.get() != null ? conversationRef.get() : fallbackId);
                response.setCreatedAt(System.currentTimeMillis());

                log.info("流式响应完成（部分）: messageId={}, answerLength={}",
                        response.getMessageId(), response.getAnswer().length());

                return response;
            }

            if (!completed) {
                log.error("流式响应超时，已收集的内容: {}", responseBuilder.toString());
                throw new RuntimeException("流式响应超时");
            }

            ChatMessageResponse response = new ChatMessageResponse();
            response.setMessageId(messageId.get());
            response.setAnswer(responseBuilder.toString());
            response.setConversationId(conversationRef.get());
            response.setCreatedAt(System.currentTimeMillis());

            log.info("流式响应完成: messageId={}, answerLength={}, conversationId={}",
                    response.getMessageId(), response.getAnswer().length(), response.getConversationId());

            return response;

        } catch (Exception e) {
            log.error("发送流式消息失败: user={}, query={}", userId, query, e);
            throw new RuntimeException("发送流式消息失败: " + e.getMessage(), e);
        }
    }

    private String extractString(Object source, String methodName) {
        if (source == null) {
            return null;
        }

        try {
            var method = source.getClass().getMethod(methodName);
            Object value = method.invoke(source);
            return value != null ? Objects.toString(value) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取会话历史
     */
    public MessageListResponse getMessageHistory(String conversationId, String userId, Integer limit) {
        try (DifyChatflowClient client = DifyClientFactory.createChatWorkflowClient(baseUrl, chatflowApiKey)) {
            return client.getMessages(conversationId, userId, null, limit != null ? limit : 20);
        } catch (Exception e) {
            log.error("获取消息历史失败: conversationId={}, userId={}", conversationId, userId, e);
            throw new RuntimeException("获取消息历史失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取用户会话列表
     */
    public ConversationListResponse getConversations(String userId, Integer limit) {
        try (DifyChatflowClient client = DifyClientFactory.createChatWorkflowClient(baseUrl, chatflowApiKey)) {
            return client.getConversations(userId, null, limit != null ? limit : 20, "updated_at");
        } catch (Exception e) {
            log.error("获取会话列表失败: userId={}", userId, e);
            throw new RuntimeException("获取会话列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除会话
     */
    public void deleteConversation(String conversationId, String userId) {
        try (DifyChatflowClient client = DifyClientFactory.createChatWorkflowClient(baseUrl, chatflowApiKey)) {
            client.deleteConversation(conversationId, userId);
            log.info("删除会话成功: conversationId={}, userId={}", conversationId, userId);
        } catch (Exception e) {
            log.error("删除会话失败: conversationId={}, userId={}", conversationId, userId, e);
            throw new RuntimeException("删除会话失败: " + e.getMessage(), e);
        }
    }

    /**
     * 重命名会话
     */
    public Conversation renameConversation(String conversationId, String name, String userId) {
        try (DifyChatflowClient client = DifyClientFactory.createChatWorkflowClient(baseUrl, chatflowApiKey)) {
            Conversation conversation = client.renameConversation(conversationId, name, false, userId);
            log.info("重命名会话成功: conversationId={}, name={}, userId={}", conversationId, name, userId);
            return conversation;
        } catch (Exception e) {
            log.error("重命名会话失败: conversationId={}, name={}, userId={}", conversationId, name, userId, e);
            throw new RuntimeException("重命名会话失败: " + e.getMessage(), e);
        }
    }
}