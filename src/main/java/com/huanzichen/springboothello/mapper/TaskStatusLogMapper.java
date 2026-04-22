package com.huanzichen.springboothello.mapper;

import com.huanzichen.springboothello.model.TaskStatusLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskStatusLogMapper {

    @Insert("insert into task_status_logs(task_id, old_status, new_status, operator_user_id, created_at) values (#{taskId}, #{oldStatus}, #{newStatus},#{operatorUserId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskStatusLog taskStatusLog);

    @Select("select id, task_id as taskId, old_status as oldStatus, new_status as newStatus, operator_user_id as operatorUserId, created_at as createdAt from task_status_logs where task_id = #{taskId} order by created_at desc")
    List<TaskStatusLog> findByTaskId(Long taskId);
}
