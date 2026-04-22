package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.common.PageResult;
import com.huanzichen.springboothello.common.Result;
import com.huanzichen.springboothello.dto.user.UserCreateDTO;
import com.huanzichen.springboothello.dto.user.UserProfileUpdateDTO;
import com.huanzichen.springboothello.dto.user.UserQueryDTO;
import com.huanzichen.springboothello.dto.user.UserUpdateDTO;
import com.huanzichen.springboothello.model.CurrentUserVO;
import com.huanzichen.springboothello.model.TaskInfo;
import com.huanzichen.springboothello.model.UserInfo;
import com.huanzichen.springboothello.service.TaskService;
import com.huanzichen.springboothello.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final TaskService taskService;

    public UserController(UserService userService, TaskService taskService) {
        this.userService = userService;
        this.taskService = taskService;
    }

    @PutMapping("/me")
    public Result<CurrentUserVO> updateCurrentUser(@RequestBody @Valid UserProfileUpdateDTO userProfileUpdateDTO) {
        return Result.success(userService.updateCurrentUser(userProfileUpdateDTO));
    }

    @GetMapping("/me/tasks")
    public Result<List<TaskInfo>> getCurrentUserTasks() {
        return Result.success(taskService.listTasks());
    }

    @PostMapping
    public Result<UserInfo> createUser(@RequestBody @Valid UserCreateDTO userCreateDTO) {
        return Result.success(userService.createUser(userCreateDTO));
    }

    @GetMapping
    public Result<List<UserInfo>> listUsers() {
        return Result.success(userService.listUsers());
    }

    @GetMapping("/search")
    public Result<List<UserInfo>> searchUsers(String name) {
        return Result.success(userService.searchUsers(name));
    }

    @GetMapping("/page")
    public Result<PageResult<UserInfo>> findUsersByPage(Integer page, Integer size, String  sort, String order) {
        return Result.success(userService.listUsersByPage(page, size, sort, order));
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<String> deleteUserById(@PathVariable Long id) {
        userService.deleteUserById(id);
        return Result.success("deleted successfully");
    }

    @GetMapping("/{id:\\d+}")
    public Result<UserInfo> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @PutMapping("/{id:\\d+}")
    public Result<UserInfo> updateUserById(@PathVariable Long id, @RequestBody @Valid UserUpdateDTO userUpdateDTO) {
        return Result.success(userService.updateUser(id, userUpdateDTO));
    }

    @GetMapping("/search-page")
    public Result<PageResult<UserInfo>> searchUsersByPage(UserQueryDTO userQueryDTO) {
        return Result.success(userService.searchUsersByPage(userQueryDTO));
    }

    @GetMapping("/{id:\\d+}/tasks")
    public Result<List<TaskInfo>> getTasksByUserId(@PathVariable Long id) {
        userService.getUserById(id);
        return Result.success(taskService.listTasksByUserId(id));
    }
}
