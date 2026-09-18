package com.project.his.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.project.his.common.Result;
import com.project.his.domain.vo.NotificationVO;
import com.project.his.service.business.INotificationBizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationBizService notificationBizService;

    @GetMapping
    public Result<List<NotificationVO>> getMyNotifications() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(notificationBizService.getUserNotifications(userId));
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(notificationBizService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public Result<Boolean> markAsRead(@PathVariable Long id) {
        notificationBizService.markAsRead(id);
        return Result.success(true);
    }

    @PutMapping("/read-all")
    public Result<Boolean> markAllAsRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        notificationBizService.markAllAsRead(userId);
        return Result.success(true);
    }
}
