package edu.smu.agent.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
        try (DifyChatflowClient client = DifyClientFactory.createChatWorkflowClient(baseUrl, chatflowApiKey)) {
            ChatMessage message = ChatMessage.builder()
                    .query(query)
                    .user(userId)
                    .responseMode(ResponseMode.STREAMING)
                    .build();

            log.info("发送流式消息: user={}, query={}", userId, query);

            // 用于等待异步回调完成
            CountDownLatch latch = new CountDownLatch(1);
            StringBuilder responseBuilder = new StringBuilder();
            AtomicReference<String> messageId = new AtomicReference<>();
            AtomicReference<Exception> exceptionRef = new AtomicReference<>();
            AtomicReference<Boolean> hasContent = new AtomicReference<>(false);

            // 发送流式消息
            client.sendChatMessageStream(message, new ChatStreamCallback() {
                @Override
                public void onMessage(MessageEvent event) {
                    log.info("收到消息片段: {}", event.getAnswer());
                    responseBuilder.append(event.getAnswer());
                    hasContent.set(true);
                }

                @Override
                public void onMessageEnd(MessageEndEvent event) {
                    log.info("消息结束，完整消息ID: {}", event.getMessageId());
                    messageId.set(event.getMessageId());
                    latch.countDown();
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
                    latch.countDown();
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("流式响应异常: {}", throwable.getMessage(), throwable);
                    exceptionRef.set(new RuntimeException("流式响应异常: " + throwable.getMessage(), throwable));
                    latch.countDown();
                }

                @Override
                public void onPing(PingEvent event) {
                    log.info("心跳: {}", event);
                }
            });

            // 等待流式响应完成，增加超时时间到60秒
            boolean completed = latch.await(60, TimeUnit.SECONDS);

            // 检查是否有异常
            Exception exception = exceptionRef.get();
            if (exception != null) {
                throw new RuntimeException("流式响应失败", exception);
            }

            // 如果没有收到 onMessageEnd 事件但有内容，仍然返回响应
            if (!completed && hasContent.get() && !responseBuilder.toString().isEmpty()) {
                log.warn("未收到 onMessageEnd 事件，但已收集到内容，返回部分响应");

                // 构建响应对象
                ChatMessageResponse response = new ChatMessageResponse();
                response.setMessageId("stream-" + System.currentTimeMillis()); // 生成临时消息ID
                response.setAnswer(responseBuilder.toString());
                response.setConversationId("stream-" + System.currentTimeMillis()); // 生成临时会话ID
                response.setCreatedAt(System.currentTimeMillis());

                log.info("流式响应完成（部分）: messageId={}, answerLength={}",
                        response.getMessageId(), response.getAnswer().length());

                return response;
            }

            if (!completed) {
                log.error("流式响应超时，已收集的内容: {}", responseBuilder.toString());
                throw new RuntimeException("流式响应超时");
            }

            // 构建响应对象
            ChatMessageResponse response = new ChatMessageResponse();
            response.setMessageId(messageId.get());
            response.setAnswer(responseBuilder.toString());
            response.setConversationId(messageId.get()); // 使用消息ID作为会话ID
            response.setCreatedAt(System.currentTimeMillis());

            log.info("流式响应完成: messageId={}, answerLength={}",
                    response.getMessageId(), response.getAnswer().length());

            return response;

        } catch (Exception e) {
            log.error("发送流式消息失败: user={}, query={}", userId, query, e);
            throw new RuntimeException("发送流式消息失败: " + e.getMessage(), e);
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