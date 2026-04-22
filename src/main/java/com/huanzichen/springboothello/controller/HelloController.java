package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.config.ProjectProperties;
import org.springframework.web.bind.annotation.*;

@RestController
public class HelloController {

    private final ProjectProperties projectProperties;

    public HelloController(ProjectProperties projectProperties) {
        this.projectProperties = projectProperties;
    }

    @GetMapping("/project-name")
    public String getProjectName() {
        return projectProperties.getName();
    }

    @GetMapping("/project-info")
    public ProjectProperties getProjectInfo() {
        return projectProperties;
    }
}
