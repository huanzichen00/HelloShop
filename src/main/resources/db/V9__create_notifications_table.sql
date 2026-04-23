create table notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    type VARCHAR(50),
    title VARCHAR(100),
    content VARCHAR(255) NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL default CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) references users(id),
    CONSTRAINT fk_notifications_order FOREIGN KEY (order_id) references orders(id),
    -- 查当前用户的通知列表
    INDEX idx_notifications_user_id (user_id),
    -- 查某用户未读通知，按时间顺序看
    INDEX idx_notifications_user_read_created_at (user_id, is_read, created_at),
    -- 按订单回查通知记录
    INDEX idx_notifications_order_id (order_id)
);
