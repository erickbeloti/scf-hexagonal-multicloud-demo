package com.example.tasks.integration;

import com.example.tasks.TasksApplication;
import com.example.tasks.adapters.inbound.http.dto.TaskDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TasksApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("local")
public class LocalProfileIntegrationTest {

    @Autowired
    WebTestClient client;

    @Test
    void create_and_get_task() {
        TaskDtos.CreateTaskRequest req = new TaskDtos.CreateTaskRequest("hello", com.example.tasks.domain.model.Priority.MEDIUM);
        var createResp = client.post().uri("/functions/createTask")
                .header("X-User-Id", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TaskDtos.TaskResponse.class)
                .returnResult().getResponseBody();
        assertThat(createResp).isNotNull();

        client.get().uri("/functions/getTaskById?id=" + createResp.id())
                .header("X-User-Id", "u1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(TaskDtos.TaskResponse.class)
                .consumeWith(res -> assertThat(res.getResponseBody()).isNotNull());
    }
}

