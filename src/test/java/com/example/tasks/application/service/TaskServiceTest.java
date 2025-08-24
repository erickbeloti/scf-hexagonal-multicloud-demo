package com.example.tasks.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.application.policy.TaskPolicies;
import com.example.tasks.domain.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TaskServiceTest {

    TaskRepositoryPort repository;
    Clock clock;
    TaskService service;
    TaskPolicies policies;

    @BeforeEach
    void setup() {
        repository = Mockito.mock(TaskRepositoryPort.class);
        clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        policies = new TaskPolicies(repository, clock);
        service = new TaskService(repository, policies, clock);
    }

    @Test
    void shouldFailWhenTooManyHighPriority() {
        when(repository.findByUserAndDateAndDescription(anyString(), any(), anyString())).thenReturn(Optional.empty());
        when(repository.countHighPriorityForUserOn(anyString(), any())).thenReturn(5L);
        assertThatThrownBy(() -> service.createTask("u", "d", Priority.HIGH))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldFailWhenDescriptionNotUnique() {
        when(repository.findByUserAndDateAndDescription(anyString(), any(), anyString())).thenReturn(Optional.of(mock(Task.class)));
        assertThatThrownBy(() -> service.createTask("u", "d", Priority.LOW))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldFailWhenTooManyOpenTasks() {
        when(repository.findByUserAndDateAndDescription(anyString(), any(), anyString())).thenReturn(Optional.empty());
        when(repository.countHighPriorityForUserOn(anyString(), any())).thenReturn(0L);
        when(repository.countOpenByUser(anyString())).thenReturn(50L);
        assertThatThrownBy(() -> service.createTask("u", "d", Priority.LOW))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotUpdateCompletedTask() {
        Task existing = new Task("1","u","d",Priority.LOW,Status.COMPLETED,LocalDateTime.now(clock),LocalDateTime.now(clock));
        when(repository.findById("1")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.updateTask("1","u","d",Priority.LOW,Status.OPEN))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAllowReadOnlyForOwner() {
        Task existing = new Task("1","other","d",Priority.LOW,Status.OPEN,LocalDateTime.now(clock),LocalDateTime.now(clock));
        when(repository.findById("1")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.getTask("1","u"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAllowDeleteOnlyForOwner() {
        Task existing = new Task("1","other","d",Priority.LOW,Status.OPEN,LocalDateTime.now(clock),LocalDateTime.now(clock));
        when(repository.findById("1")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.deleteTask("1","u"))
            .isInstanceOf(IllegalStateException.class);
    }
}
