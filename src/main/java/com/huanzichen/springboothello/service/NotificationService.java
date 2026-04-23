package com.huanzichen.springboothello.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.common.UserContext;
import com.huanzichen.springboothello.exception.BusinessException;
import com.huanzichen.springboothello.mapper.NotificationMapper;
import com.huanzichen.springboothello.model.Notification;
import org.apache.logging.log4j.message.LoggerNameAwareMessage;
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
        notification.setType("ORDER_CREATED");
        notification.setTitle("订单创建成功");
        notification.setContent("您的订单 " + orderNo + " 已创建成功");
        notification.setIsRead(false);

        notificationMapper.insert(notification);
    }
}
