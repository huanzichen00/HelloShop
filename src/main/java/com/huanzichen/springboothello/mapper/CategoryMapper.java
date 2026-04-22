package com.huanzichen.springboothello.mapper;

import com.huanzichen.springboothello.model.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("select id, name, sort, created_at as createdAt from categories order by sort asc, id asc")
    List<Category> findAll();
}
