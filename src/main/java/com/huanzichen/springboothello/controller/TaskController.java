package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.common.PageResult;
import com.huanzichen.springboothello.common.Result;
import com.huanzichen.springboothello.dto.task.TaskCreateDTO;
import com.huanzichen.springboothello.dto.task.TaskQueryDTO;
import com.huanzichen.springboothello.dto.task.TaskUpdateDTO;
import com.huanzichen.springboothello.model.TaskInfo;
import com.huanzichen.springboothello.model.TaskStatusLog;
import com.huanzichen.springboothello.model.TaskStatusSummaryVO;
import com.huanzichen.springboothello.model.UserInfo;
import com.huanzichen.springboothello.service.TaskService;
import com.huanzichen.springboothello.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private final TaskService taskService;
    private final UserService userService;

    public TaskController(TaskService taskService, UserService userService) {
        this.taskService = taskService;
        this.userService = userService;
    }

    @PostMapping
    public Result<TaskInfo> createTask(@RequestBody @Valid TaskCreateDTO taskCreateDTO) {
        return Result.success(taskService.createTask(taskCreateDTO));
    }

    @GetMapping
    public Result<List<TaskInfo>> getAllTasks() {
        return Result.success(taskService.listTasks());
    }

    @GetMapping("/search-page")
    public Result<PageResult<TaskInfo>> searchTasksByPage(TaskQueryDTO queryDTO) {
        return Result.success(taskService.searchTasksByPage(queryDTO));
    }

    @GetMapping("/status-summary")
    public Result<TaskStatusSummaryVO> getTaskStatusSummary() {
        return Result.success(taskService.getTaskStatusSummary());
    }

    @GetMapping("/{id:\\d+}")
    public Result<TaskInfo> getTaskById(@PathVariable Long id) {
        return Result.success(taskService.getTaskById(id));
    }

    @PutMapping("/{id:\\d+}")
    public Result<TaskInfo> updateTask(@PathVariable Long id, @RequestBody @Valid TaskUpdateDTO taskUpdateDTO) {
        return Result.success(taskService.updateTask(id, taskUpdateDTO));
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<String> deleteTask(@PathVariable Long id) {
        taskService.deleteTaskById(id);
        return Result.success("deleted successfully");
    }

    @GetMapping("/{id:\\d+}/user")
    public Result<UserInfo> getUserById(@PathVariable Long id) {
        TaskInfo taskInfo = taskService.getTaskById(id);
        return Result.success(userService.getUserById(taskInfo.getUserId()));
    }

    @GetMapping("/{id:\\d+}/status-logs")
    public Result<List<TaskStatusLog>> getTaskStatusLogs(@PathVariable Long id) {
        return Result.success(taskService.listTaskStatusLogs(id));
    }
}
