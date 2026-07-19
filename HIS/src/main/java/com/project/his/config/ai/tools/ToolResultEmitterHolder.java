package com.project.his.config.ai.tools;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.project.his.config.ai.event.ToolEventEmitter;

/**
 * 跨线程传递 ToolEventEmitter，使用阿里 TransmittableThreadLocal。
 */
public class ToolResultEmitterHolder {

    private static final TransmittableThreadLocal<ToolEventEmitter> EMITTER = new TransmittableThreadLocal<>();

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
