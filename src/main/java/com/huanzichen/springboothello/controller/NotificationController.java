package com.huanzichen.springboothello.controller;

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

    @PutMapping("/{id}/read")
    public Result<Notification> markMyNotificationAsRead(@PathVariable Long id) {
        return Result.success(notificationService.markMyNotificationAsRead(id));
    }
}
