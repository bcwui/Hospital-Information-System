package com.project.his.common.ai.memory;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 基于 Redisson 的 ChatMemoryRepository 实现
 */
public class RedissonChatMemoryRepository implements ChatMemoryRepository {

    private final RedissonClient redissonClient;
    private final String keyPrefix;
    private final Duration ttl;
    private final int maxMessages;
    private final String conversationSetKey;

    public RedissonChatMemoryRepository(RedissonClient redissonClient, String keyPrefix, Duration ttl, int maxMessages) {
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient不能为空");
        this.keyPrefix = StringUtils.hasText(keyPrefix) ? keyPrefix : "ai-chat-memory-";
        this.ttl = ttl;
        this.maxMessages = maxMessages;
        this.conversationSetKey = this.keyPrefix + "conversation-ids";
    }

    @Override
    public List<String> findConversationIds() {
        RSet<String> idSet = redissonClient.getSet(conversationSetKey);
        if (idSet.isEmpty()) {
            return List.of();
        }
        Set<String> raw = idSet.readAll();
        List<String> result = new ArrayList<>(raw.size());
        for (String conversationId : raw) {
            if (!StringUtils.hasText(conversationId)) {
                idSet.remove(conversationId);
                continue;
            }
            if (redissonClient.getBucket(buildKey(conversationId)).isExists()) {
                result.add(conversationId);
            } else {
                idSet.remove(conversationId);
            }
        }
        return result;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return List.of();
        }
        RBucket<List<ChatMemoryEntry>> bucket = redissonClient.getBucket(buildKey(conversationId));
        List<ChatMemoryEntry> entries = bucket.get();
        if (CollectionUtils.isEmpty(entries)) {
            return List.of();
        }
        return toMessages(entries);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        List<ChatMemoryEntry> entries = toEntries(messages);
        if (maxMessages > 0 && entries.size() > maxMessages) {
            entries = entries.subList(entries.size() - maxMessages, entries.size());
        }
        RBucket<List<ChatMemoryEntry>> bucket = redissonClient.getBucket(buildKey(conversationId));
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            bucket.set(entries, ttl);
        } else {
            bucket.set(entries);
        }
        redissonClient.getSet(conversationSetKey).add(conversationId);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        redissonClient.getBucket(buildKey(conversationId)).delete();
        redissonClient.getSet(conversationSetKey).remove(conversationId);
    }

    private String buildKey(String conversationId) {
        return keyPrefix + conversationId;
    }

    private List<ChatMemoryEntry> toEntries(List<Message> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return List.of();
        }
        List<ChatMemoryEntry> entries = new ArrayList<>(messages.size());
        for (Message message : messages) {
            if (message == null) {
                continue;
            }
            Map<String, Object> metadata = message.getMetadata();
            if (metadata == null || metadata.isEmpty()) {
                metadata = Map.of();
            } else {
                metadata = new HashMap<>(metadata);
            }
            String content = message.getText();
            if (content == null) {
                content = "";
            }
            entries.add(new ChatMemoryEntry(message.getMessageType(), content, metadata));
        }
        return entries;
    }

    private List<Message> toMessages(List<ChatMemoryEntry> entries) {
        List<Message> messages = new ArrayList<>(entries.size());
        for (ChatMemoryEntry entry : entries) {
            Message message = toMessage(entry);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    private Message toMessage(ChatMemoryEntry entry) {
        if (entry == null) {
            return null;
        }
        MessageType type = entry.getType();
        String content = entry.getContent();
        if (!StringUtils.hasText(content) && StringUtils.hasText(entry.getText())) {
            content = entry.getText();
        }
        if (content == null) {
            content = "";
        }
        Map<String, Object> metadata = entry.getMetadata();
        if (metadata == null) {
            metadata = Map.of();
        }
        if (type == null) {
            return UserMessage.builder().text(content).metadata(metadata).build();
        }
        return switch (type) {
            case SYSTEM -> SystemMessage.builder().text(content).metadata(metadata).build();
            case ASSISTANT -> new AssistantMessage(content, metadata);
            case USER -> UserMessage.builder().text(content).metadata(metadata).build();
            case TOOL -> new AssistantMessage(content, metadata);
            default -> new AssistantMessage(content, metadata);
        };
    }

    @Data
    @NoArgsConstructor
    public static class ChatMemoryEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        private MessageType type;

        private String content;

        private String text;

        private Map<String, Object> metadata;

        public ChatMemoryEntry(MessageType type, String content, Map<String, Object> metadata) {
            this.type = type;
            this.content = content;
            this.metadata = metadata;
        }
    }
}
