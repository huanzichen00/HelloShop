package com.huanzichen.springboothello;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SpringbootHelloApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void createUserShouldAcceptJsonBody() throws Exception {
        String username = "create_user_" + System.currentTimeMillis();
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "123456",
                                  "name": "huanzichen",
                                  "age": 18
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "code": 200,
                          "message": "success",
                          "data": {
                            "username": "%s",
                            "name": "huanzichen",
                            "age": 18
                          }
                        }
                        """.formatted(username), false));
    }

}
