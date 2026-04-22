package com.huanzichen.springboothello.model;

public class TaskStatusSummaryVO {

    private Long todoCount;
    private Long doneCount;

    public Long getTodoCount() {
        return todoCount;
    }

    public void setTodoCount(Long todoCount) {
        this.todoCount = todoCount;
    }

    public Long getDoneCount() {
        return doneCount;
    }

    public void setDoneCount(Long doneCount) {
        this.doneCount = doneCount;
    }
}
