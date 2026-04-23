package com.huanzichen.springboothello.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huanzichen.springboothello.model.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
