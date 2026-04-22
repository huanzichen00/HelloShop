CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(100) NOT NULL,
    product_price DECIMAL(10, 2) NOT NULL,
    product_cover_url VARCHAR(255),
    quantity INT NOT NULL,
    subtotal_amount DECIMAL(10, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    constraint fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_order_items_order_id (order_id),
    INDEX idx_order_items_product_id (product_id)

)