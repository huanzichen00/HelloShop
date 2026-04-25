package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.common.PageResult;
import com.huanzichen.springboothello.common.Result;
import com.huanzichen.springboothello.model.Notification;
import com.huanzichen.springboothello.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<List<Notification>> listMyNotifications() {
        return Result.success(notificationService.listMyNotifications());
    }

    @GetMapping("/page")
    public Result<PageResult<Notification>> listMyNotificationsByPage(Integer page, Integer size) {
        return Result.success(notificationService.listMyNotificationsByPage(page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> countMyUnreadNotifications() {
        return Result.success(notificationService.countMyUnreadNotifications());
    }

    @PutMapping("/read-all")
    public Result<Long> markAllMyNotificationsAsRead() {
        return Result.success(notificationService.markAllMyNotificationsAsRead());
    }

    @PutMapping("/{id}/read")
    public Result<Notification> markMyNotificationAsRead(@PathVariable Long id) {
        return Result.success(notificationService.markMyNotificationAsRead(id));
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> deleteMyNotification(@PathVariable Long id) {
        notificationService.deleteMyNotification(id);
        return Result.success();
    }
}
