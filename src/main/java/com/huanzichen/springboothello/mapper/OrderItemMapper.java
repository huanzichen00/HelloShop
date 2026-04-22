package com.huanzichen.springboothello.mapper;

import com.huanzichen.springboothello.model.Order;
import com.huanzichen.springboothello.model.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    @Insert("""
            insert into order_items(order_id, product_id, product_name,
                                    product_price, product_cover_url, quantity, subtotal_amount) 
            values (#{orderId}, #{productId}, #{productName}, #{productPrice},
                    #{productCoverUrl}, #{quantity}, #{subtotalAmount})
            """)
    int insert(OrderItem orderItem);

    @Select("""
              select id,
                     order_id as orderId,
                     product_id as productId,
                     product_name as productName,
                     product_price as productPrice,
                     product_cover_url as productCoverUrl,
                     quantity,
                     subtotal_amount as subtotalAmount,
                     created_at as createdAt
              from order_items
              where order_id = #{orderId}
              order by id asc
              """)
    List<OrderItem> findByOrderId(Long orderId);
}
