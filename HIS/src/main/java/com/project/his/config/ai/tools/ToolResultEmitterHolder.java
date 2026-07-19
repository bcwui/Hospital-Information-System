package com.project.his.config.ai.tools;

import com.project.his.config.ai.event.ToolEventEmitter;

/**
 * 跨线程传递 ToolEventEmitter。
 * AIServiceImpl 在每次请求时设置，Tool 方法通过此 Holder 获取。
 */
public class ToolResultEmitterHolder {

    private static final ThreadLocal<ToolEventEmitter> EMITTER = new ThreadLocal<>();

    public static void set(ToolEventEmitter emitter) {
        EMITTER.set(emitter);
    }

    public static ToolEventEmitter get() {
        return EMITTER.get();
    }

    public static void clear() {
        EMITTER.remove();
    }

    private ToolResultEmitterHolder() {}
}
