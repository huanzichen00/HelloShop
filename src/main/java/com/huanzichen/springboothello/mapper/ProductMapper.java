package com.huanzichen.springboothello.mapper;

import com.huanzichen.springboothello.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    Product findById(Long id);

    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    List<Product> searchWithConditionsAndPage(@Param("categoryId") Long categoryId,
                                              @Param("keyword") String keyword,
                                              @Param("offset") Integer offset,
                                              @Param("size") Integer size,
                                              @Param("sort") String sort,
                                              @Param("order") String order);

    Long countWithConditions(@Param("categoryId") Long categoryId,
                             @Param("keyword") String keyword);

    int restoreStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
