package com.project.his.service.business;

import com.project.his.domain.vo.NotificationVO;

import java.util.List;

public interface INotificationBizService {

    void sendNotification(Long userId, String title, String content, String type, Long relatedId);

    void sendAppointmentNotification(Long userId, String title, String content, Long appointmentId);

    List<NotificationVO> getUserNotifications(Long userId);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userId);

}
