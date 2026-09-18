package com.project.his.service.business.impl;

import com.project.his.domain.po.Notification;
import com.project.his.domain.po.User;
import com.project.his.domain.vo.NotificationVO;
import com.project.his.service.business.INotificationBizService;
import com.project.his.service.entity.INotificationService;
import com.project.his.service.entity.IUserService;
import com.project.his.service.entity.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBizServiceImpl implements INotificationBizService {

    private final INotificationService notificationService;
    private final IUserService userService;
    private final MailService mailService;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long userId, String title, String content, String type, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notificationService.save(notification);
        log.info("站内通知已发送: userId={}, title={}, type={}", userId, title, type);

        if (!mailEnabled) return;

        try {
            User user = userService.getById(userId);
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                mailService.sendSimpleMail(user.getEmail(),
                        "【智慧医院】" + title, content);
            }
        } catch (Exception e) {
            log.warn("邮件发送失败（站内通知已保存）: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Override
    public void sendAppointmentNotification(Long userId, String title, String content, Long appointmentId) {
        sendNotification(userId, title, content, "appointment_reminder", appointmentId);
    }

    @Override
    public List<NotificationVO> getUserNotifications(Long userId) {
        return notificationService.getByUserId(userId).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationService.getUnreadCount(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId) {
        Notification notification = notificationService.getById(notificationId);
        if (notification != null) {
            notification.setIsRead(1);
            notificationService.updateById(notification);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationService.getUnreadByUserId(userId);
        for (Notification n : unread) {
            n.setIsRead(1);
            notificationService.updateById(n);
        }
    }

    private NotificationVO toVO(Notification n) {
        return NotificationVO.builder()
                .notificationId(n.getNotificationId())
                .title(n.getTitle())
                .content(n.getContent())
                .type(n.getType())
                .isRead(n.getIsRead())
                .relatedId(n.getRelatedId())
                .createTime(n.getCreateTime())
                .build();
    }
}
