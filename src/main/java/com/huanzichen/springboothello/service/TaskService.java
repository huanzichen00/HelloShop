package com.huanzichen.springboothello.service;

import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.common.PageResult;
import com.huanzichen.springboothello.common.UserContext;
import com.huanzichen.springboothello.dto.task.TaskCreateDTO;
import com.huanzichen.springboothello.dto.task.TaskQueryDTO;
import com.huanzichen.springboothello.dto.task.TaskUpdateDTO;
import com.huanzichen.springboothello.exception.BusinessException;
import com.huanzichen.springboothello.mapper.TaskMapper;
import com.huanzichen.springboothello.mapper.TaskStatusLogMapper;
import com.huanzichen.springboothello.model.TaskInfo;
import com.huanzichen.springboothello.model.TaskStatusLog;
import com.huanzichen.springboothello.model.TaskStatusSummaryVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskStatusLogMapper taskStatusLogMapper;

    public TaskService(TaskMapper taskMapper, TaskStatusLogMapper taskStatusLogMapper) {
        this.taskMapper = taskMapper;
        this.taskStatusLogMapper = taskStatusLogMapper;
    }

    public TaskInfo createTask(TaskCreateDTO taskCreateDTO) {

        TaskInfo taskInfo = new TaskInfo();

        Long currentUserId = UserContext.getCurrentUserId();

        fillTaskInfo(taskInfo,
                taskCreateDTO.getTitle(),
                taskCreateDTO.getDescription(),
                taskCreateDTO.getStatus(),
                currentUserId);

        taskMapper.insert(taskInfo);

        return taskInfo;
    }

    public List<TaskInfo> listTasks() {
        Long userId = UserContext.getCurrentUserId();
        return taskMapper.findByUserId(userId);
    }

    public TaskInfo getTaskById(Long id) {
        return getOwnedTask(id);
    }

    public TaskInfo updateTask(Long id, TaskUpdateDTO taskUpdateDTO) {

        TaskInfo taskInfo = getOwnedTask(id);

        String oldStatus = taskInfo.getStatus();

        updateCompletedAt(taskInfo, taskUpdateDTO.getStatus());

        fillTaskInfo(taskInfo,
                taskUpdateDTO.getTitle(),
                taskUpdateDTO.getDescription(),
                taskUpdateDTO.getStatus(),
                UserContext.getCurrentUserId());

        int rows = taskMapper.update(taskInfo);
        validateRows(rows);

        String newStatus = taskUpdateDTO.getStatus();
        if (!oldStatus.equals(newStatus)) {
            saveTaskStatusLog(taskInfo.getId(), oldStatus, newStatus);
        }

        return taskInfo;
    }

    public void deleteTaskById(Long id) {
        getOwnedTask(id);
        int rows = taskMapper.deleteById(id);
        validateRows(rows);
    }

    public PageResult<TaskInfo> searchTasksByPage(TaskQueryDTO queryDTO) {
        Integer page = queryDTO.getPage();
        Integer size = queryDTO.getSize();
        String sort = normalizeSort(queryDTO.getSort());
        String order = normalizeOrder(queryDTO.getOrder());

        validatePageParams(page, size);

        int offset = (page - 1) * size;
        List<TaskInfo> list = taskMapper.searchWithConditionsAndPage(
                queryDTO.getTitle(),
                queryDTO.getStatus(),
                queryDTO.getUserId(),
                offset,
                size,
                sort,
                order
        );
        Long total = taskMapper.countWithConditions(
                queryDTO.getTitle(),
                queryDTO.getStatus(),
                queryDTO.getUserId()
        );

        return buildPageResult(list, total, page, size);
    }

    public List<TaskInfo> listTasksByUserId(Long userId) {
        return taskMapper.findByUserId(userId);
    }

    public TaskStatusSummaryVO getTaskStatusSummary() {
        Long userId = UserContext.getCurrentUserId();

        TaskStatusSummaryVO taskStatusSummaryVO = new TaskStatusSummaryVO();
        Long todoCount = taskMapper.countTodoByUserId(userId);
        Long doneCount = taskMapper.countDoneByUserId(userId);
        taskStatusSummaryVO.setTodoCount(todoCount);
        taskStatusSummaryVO.setDoneCount(doneCount);
        return taskStatusSummaryVO;
    }

    public List<TaskStatusLog> listTaskStatusLogs(Long taskId) {
        getOwnedTask(taskId);
        return  taskStatusLogMapper.findByTaskId(taskId);
    }

    private void fillTaskInfo(TaskInfo taskInfo, String title, String description, String status, Long userId) {
        validateStatus(status);
        taskInfo.setTitle(title);
        taskInfo.setDescription(description);
        taskInfo.setStatus(status);
        taskInfo.setUserId(userId);
    }

    private void updateCompletedAt(TaskInfo taskInfo, String newStatus) {
        String oldStatus = taskInfo.getStatus();

        if ("DONE".equals(newStatus)) {
            if (!"DONE".equals(oldStatus) || taskInfo.getCompletedAt() == null) {
                taskInfo.setCompletedAt(LocalDateTime.now());
            }
        } else {
            taskInfo.setCompletedAt(null);
        }
    }

    private void saveTaskStatusLog(Long taskId, String oldStatus, String newStatus) {
        Long operatorId = UserContext.getCurrentUserId();

        TaskStatusLog taskStatusLog = new TaskStatusLog();
        taskStatusLog.setTaskId(taskId);
        taskStatusLog.setOldStatus(oldStatus);
        taskStatusLog.setNewStatus(newStatus);
        taskStatusLog.setOperatorUserId(operatorId);
        taskStatusLog.setCreatedAt(LocalDateTime.now());

        taskStatusLogMapper.insert(taskStatusLog);
    }

    private void validateStatus(String status) {
        if (!"TODO".equals(status) && !"DONE".equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "invalid task status");
        }
    }

    private void validateRows(int rows) {
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "task not found");
        }
    }

    private void validateTask(TaskInfo taskInfo) {
        if (taskInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "task not found");
        }
    }

    private void validatePageParams(Integer page, Integer size) {
        if (page == null || page <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "page must be greater than zero");
        }
        if (size == null || size <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "size must be greater than zero");
        }
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "id";
        }
        if (!"id".equals(sort)) {
            return "id";
        }
        return sort;
    }

    private String normalizeOrder(String order) {
        if (order == null || order.trim().isEmpty()) {
            return "asc";
        }
        order = order.toLowerCase();
        if (!"asc".equals(order) && !"desc".equals(order)) {
            return "asc";
        }
        return order;
    }

    private PageResult<TaskInfo> buildPageResult(List<TaskInfo> list, Long total, Integer page, Integer size) {
        int totalPages = (int) ((total + size - 1) / size);
        return new PageResult<>(total, list, page, size, totalPages);
    }

    private TaskInfo getOwnedTask(Long id) {
        TaskInfo taskInfo = taskMapper.findById(id);
        validateTask(taskInfo);

        Long currentUserId = UserContext.getCurrentUserId();
        if (!taskInfo.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "no permission");
        }

        return taskInfo;
    }
}
