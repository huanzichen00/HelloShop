package com.huanzichen.springboothello.mapper;

import com.huanzichen.springboothello.model.TaskInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TaskMapper {
    @Insert("insert into tasks(title, description, status, user_id, completed_at) values (#{title}, #{description}, #{status}, #{userId}, #{completedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskInfo taskInfo);

    @Select("select id, title, description, status, user_id as userId, completed_at as completedAt from tasks")
    List<TaskInfo> findAll();

    @Select("select id, title, description, status, user_id as userId, completed_at as completedAt from tasks where id = #{id}")
    TaskInfo findById(Long id);

    @Update("update tasks set title = #{title}, description = #{description}, status = #{status}, completed_at = #{completedAt} where id = #{id}")
    int update(TaskInfo taskInfo);

    @Delete("delete from tasks where id = #{id}")
    int deleteById(Long id);

    List<TaskInfo> searchWithConditionsAndPage(@Param("title") String title,
                                               @Param("status") String status,
                                               @Param("userId") Long userId,
                                               @Param("offset") Integer offset,
                                               @Param("size") Integer size,
                                               @Param("sort") String sort,
                                               @Param("order") String order);

    Long countWithConditions(@Param("title") String title,
                             @Param("status") String status,
                             @Param("userId") Long userId);

    @Select("select id, title, description, status, user_id as userId, completed_at as completedAt from tasks where user_id = #{userId}")
    List<TaskInfo> findByUserId(Long userId);

    @Select("select count(*) from tasks where user_id = #{userId} and status = 'TODO'")
    Long countTodoByUserId(Long userId);

    @Select("select count(*) from tasks where user_id = #{userId} and status = 'DONE'")
    Long countDoneByUserId(Long userId);
}
