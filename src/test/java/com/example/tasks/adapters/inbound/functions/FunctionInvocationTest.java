package com.example.tasks.adapters.inbound.functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.example.tasks.adapters.inbound.functions.dto.CreateTaskRequest;
import com.example.tasks.adapters.inbound.functions.dto.TaskResponse;
import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.*;
import java.time.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FunctionInvocationTest {

    @Autowired
    FunctionCatalog catalog;

    @MockBean
    TaskRepositoryPort repository;

    @Autowired
    Clock clock;

    @Test
    void createTaskFunctionWorks() {
        when(repository.findByUserAndDateAndDescription(anyString(), any(), anyString())).thenReturn(java.util.Optional.empty());
        when(repository.countHighPriorityForUserOn(anyString(), any())).thenReturn(0L);
        when(repository.countOpenByUser(anyString())).thenReturn(0L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Function<CreateTaskRequest, TaskResponse> func = catalog.lookup("createTask");
        TaskResponse response = func.apply(new CreateTaskRequest("u", "d", Priority.LOW));
        assertThat(response.description()).isEqualTo("d");
        assertThat(response.status()).isEqualTo(Status.OPEN);
    }
}
