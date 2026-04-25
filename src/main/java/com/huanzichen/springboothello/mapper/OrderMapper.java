package com.huanzichen.springboothello.mapper;

import com.huanzichen.springboothello.model.Order;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("""
            insert into orders (order_no, user_id, status, total_amount, total_quantity) 
            values (#{orderNo}, #{userId}, #{status}, #{totalAmount}, #{totalQuantity})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Select("""
            select id,
                   order_no as orderNo,
                   user_id as userId,
                   status,
                   total_amount as totalAmount,
                   total_quantity as totalQuantity,
                   created_at as createdAt,
                   updated_at as updatedAt
            from orders
            where id = #{id}
            """)
    Order findById(Long id);

    @Select("""
              select id,
                     order_no as orderNo,
                     user_id as userId,
                     status,
                     total_amount as totalAmount,
                     total_quantity as totalQuantity,
                     created_at as createdAt,
                     updated_at as updatedAt
              from orders
              where user_id = #{userId}
              order by created_at desc, id desc
              """)
    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdPage(@Param("userId") Long userId,
                                 @Param("status") String status,
                                 @Param("offset") Integer offset,
                                 @Param("size") Integer size,
                                 @Param("sort") String sort,
                                 @Param("order") String order);

    Long countByUserId(@Param("userId") Long userId,
                       @Param("status") String status);

    @Select("""
              select id,
                     order_no as orderNo,
                     user_id as userId,
                     status,
                     total_amount as totalAmount,
                     total_quantity as totalQuantity,
                     created_at as createdAt,
                     updated_at as updatedAt
              from orders
              where id = #{id}
                and user_id = #{userId}
              """)
    Order findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Update("""
            update orders
            set status = #{status},
                updated_at = now()
            where id = #{id}
            """)
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    @Update("""
            update orders
            set status = #{newStatus},
                updated_at = now()
            where id = #{id}
              and status = #{currentStatus}
            """)
    int updateStatusByIdAndCurrentStatus(@Param("id") Long id,
                                         @Param("currentStatus") String currentStatus,
                                         @Param("newStatus") String newStatus);

    @Select("""
          select id,
                 order_no as orderNo,
                 user_id as userId,
                 status,
                 total_amount as totalAmount,
                 total_quantity as totalQuantity,
                 created_at as createdAt,
                 updated_at as updatedAt
          from orders
          where status = 'PENDING_PAYMENT'
            and created_at < #{deadline}
          order by id asc
          """)
    List<Order> findTimeoutPendingOrders(@Param("deadline") LocalDateTime deadline);
}
