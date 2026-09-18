package com.project.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    private Long notificationId;

    private String title;

    private String content;

    private String type;

    private Integer isRead;

    private Long relatedId;

    private LocalDateTime createTime;

}
