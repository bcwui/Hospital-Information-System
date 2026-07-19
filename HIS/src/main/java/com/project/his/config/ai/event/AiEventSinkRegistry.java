package com.project.his.config.ai.event;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class AiEventSinkRegistry {

    private static final ConcurrentMap<String, Sinks.Many<AiEvent>> CHANNELS = new ConcurrentHashMap<>();

    public static void register(String sessionId, Sinks.Many<AiEvent> sink) {
        CHANNELS.put(sessionId, sink);
        log.debug("注册事件通道: sessionId={}", sessionId);
    }

    public static Sinks.Many<AiEvent> get(String sessionId) {
        return CHANNELS.get(sessionId);
    }

    public static void remove(String sessionId) {
        CHANNELS.remove(sessionId);
        log.debug("移除事件通道: sessionId={}", sessionId);
    }
}
