package com.huanzichen.springboothello.dto.task;

import jakarta.validation.constraints.NotBlank;

public class TaskUpdateDTO {
    @NotBlank(message = "title cannot be blank")
    private String title;
    private String description;
    @NotBlank(message = "status cannot be blank")
    private String status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
