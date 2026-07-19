package com.project.his.config.ai.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEvent {

    private String type;     // text | tool_call | tool_result | card | ref | done | error

    // text
    private String content;

    // tool_call / tool_result
    private String toolName;
    private String toolLabel;
    private String status;   // running | success | error

    // card
    private String cardType;  // doctor | schedule | appointment_result
    private Object data;

    // ref
    private List<RefSource> sources;

    // done
    private String sessionId;

    // error
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefSource {
        private String title;
        private String snippet;
    }
}
