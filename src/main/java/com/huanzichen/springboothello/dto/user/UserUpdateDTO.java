package com.huanzichen.springboothello.dto.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserUpdateDTO {
    @NotBlank(message = "name cannot be blank")
    private String name;

    @NotNull(message = "age cannot be null")
    @Min(value = 0, message = "age cannot be negative")
    private Integer age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
