package com.project.his.service.entity;

import com.project.his.domain.po.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface INotificationService extends IService<Notification> {

    List<Notification> getByUserId(Long userId);

    List<Notification> getUnreadByUserId(Long userId);

    long getUnreadCount(Long userId);

}
