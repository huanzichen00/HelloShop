package com.huanzichen.springboothello.mapper;

import com.huanzichen.springboothello.model.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Insert("""
            insert into categories(name, sort)
            values (#{name}, #{sort})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    @Select("select id, name, sort, created_at as createdAt from categories order by sort asc, id asc")
    List<Category> findAll();

    @Select("""
            select id,
                   name,
                   sort,
                   created_at as createdAt
            from categories
            where id = #{id}
            """)
    Category findById(Long id);

    @Delete("""
            delete from categories
            where id = #{id}
            """)
    int deleteById(Long id);

    @Select("""
            select count(*)
            from products
            where category_id = #{categoryId}
            """)
    Long countProductsByCategoryId(Long categoryId);
}
