package com.huanzichen.springboothello;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn401WhenAccessingTasksWithoutLogin() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("please login first"));
    }

    @Test
    void shouldReturn403WhenAccessingOtherUsersTask() throws Exception {
        String ownerUsername = "task_owner_" + System.currentTimeMillis();
        String visitorUsername = "task_visitor_" + System.currentTimeMillis();

        String ownerRegisterBody = """
                {
                  "username": "%s",
                  "password": "123456",
                  "name": "task owner",
                  "age": 20
                }
                """.formatted(ownerUsername);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerRegisterBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String visitorRegisterBody = """
                {
                  "username": "%s",
                  "password": "123456",
                  "name": "task visitor",
                  "age": 20
                }
                """.formatted(visitorUsername);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(visitorRegisterBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String ownerLoginBody = """
                {
                  "username": "%s",
                  "password": "123456"
                }
                """.formatted(ownerUsername);

        String ownerLoginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerLoginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String ownerToken = ownerLoginResponse.split("\"data\":\"")[1].split("\"")[0];

        String createTaskBody = """
                {
                  "title": "forbidden task",
                  "description": "owner only",
                  "status": "TODO"
                }
                """;

        String createTaskResponse = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTaskBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = createTaskResponse.split("\"id\":")[1].split(",")[0].trim();

        String visitorLoginBody = """
                {
                  "username": "%s",
                  "password": "123456"
                }
                """.formatted(visitorUsername);

        String visitorLoginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(visitorLoginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String visitorToken = visitorLoginResponse.split("\"data\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + visitorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("no permission"));
    }
}
