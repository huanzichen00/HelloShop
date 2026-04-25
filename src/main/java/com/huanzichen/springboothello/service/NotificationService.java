package com.huanzichen.springboothello.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.common.NotificationType;
import com.huanzichen.springboothello.common.PageResult;
import com.huanzichen.springboothello.common.UserContext;
import com.huanzichen.springboothello.exception.BusinessException;
import com.huanzichen.springboothello.mapper.NotificationMapper;
import com.huanzichen.springboothello.model.Notification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    public List<Notification> listMyNotifications() {
        Long userId = UserContext.getCurrentUserId();

        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .orderByDesc(Notification::getId);
        return notificationMapper.selectList(queryWrapper);
    }

    public PageResult<Notification> listMyNotificationsByPage(Integer page, Integer size) {
        validatePageParams(page, size);
        Long userId = UserContext.getCurrentUserId();
        int offset = (page - 1) * size;

        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .orderByDesc(Notification::getId)
                .last("limit " + offset + ", " + size);

        List<Notification> list = notificationMapper.selectList(queryWrapper);

        LambdaQueryWrapper<Notification> countQueryWrapper = new LambdaQueryWrapper<>();
        countQueryWrapper.eq(Notification::getUserId, userId);
        Long total = notificationMapper.selectCount(countQueryWrapper);

        int totalPages = (int) ((total + size - 1) / size);
        return new PageResult<>(total, list, page, size, totalPages);
    }

    public Notification markMyNotificationAsRead(Long id) {
        Long userId = UserContext.getCurrentUserId();

        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getId, id)
                .eq(Notification::getUserId, userId);

        Notification notification = notificationMapper.selectOne(queryWrapper);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "notification not found");
        }

        notification.setIsRead(true);
        notificationMapper.updateById(notification);
        return notificationMapper.selectById(id);
    }

    public void createOrderCreatedNotification(Long userId, Long orderId, String orderNo) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setOrderId(orderId);
        notification.setType(NotificationType.ORDER_CREATED);
        notification.setTitle("订单创建成功");
        notification.setContent("您的订单 " + orderNo + " 已创建成功");
        notification.setIsRead(false);

        notificationMapper.insert(notification);
    }

    public Long countMyUnreadNotifications() {
        Long userId = UserContext.getCurrentUserId();

        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false);

        return notificationMapper.selectCount(queryWrapper);
    }

    public Long markAllMyNotificationsAsRead() {
        Long userId = UserContext.getCurrentUserId();
        Long unreadCount = countMyUnreadNotifications();
        if (unreadCount == 0) {
            return 0L;
        }

        Notification notification = new Notification();
        notification.setIsRead(true);

        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false);

        notificationMapper.update(notification, updateWrapper);

        return unreadCount;
    }

    public void deleteMyNotification(Long id) {
        Long userId = UserContext.getCurrentUserId();

        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getId, id)
                .eq(Notification::getUserId, userId);

        Notification notification = notificationMapper.selectOne(queryWrapper);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "notification not found");
        }

        notificationMapper.deleteById(id);
    }

    public void createOrderTimeoutCanceledNotification(Long userId, Long orderId, String orderNo) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setOrderId(orderId);
        notification.setType(NotificationType.ORDER_TIMEOUT_CANCELED);
        notification.setTitle("订单已超时取消");
        notification.setContent("您的订单 " + orderNo + " 因超时未支付已自动取消");
        notification.setIsRead(false);

        notificationMapper.insert(notification);
    }

    private static void validatePageParams(Integer page, Integer size) {
        if (page == null || page <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "page must be greater than zero");
        }
        if (size == null || size <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "size must be greater than zero");
        }
    }
}
