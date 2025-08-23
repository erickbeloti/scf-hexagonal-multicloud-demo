package com.example.tasks.application.service;

import com.example.tasks.application.port.outbound.ClockPort;
import com.example.tasks.application.port.outbound.IdGeneratorPort;
import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.model.Priority;
import com.example.tasks.domain.model.Status;
import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.policy.TaskCreationPolicy;
import com.example.tasks.domain.policy.TaskUpdatePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TaskServiceTest {
    TaskRepositoryPort repository;
    ClockPort clockPort;
    IdGeneratorPort idGen;
    TaskService service;

    @BeforeEach
    void setup() {
        repository = mock(TaskRepositoryPort.class);
        clockPort = () -> Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        idGen = () -> "id-1";
        service = new TaskService(repository, clockPort, idGen, new TaskCreationPolicy(), new TaskUpdatePolicy());
    }

    @Test
    void create_enforces_rules_and_saves() {
        when(repository.countHighPriorityForUserOn(any(), anyString())).thenReturn(0L);
        when(repository.findByUserAndDateAndDescription(anyString(), any(), anyString())).thenReturn(Optional.empty());
        when(repository.countOpenByUser(anyString())).thenReturn(0L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Task draft = new Task(null, "u1", "desc", Priority.HIGH, Status.OPEN, Instant.now(), Instant.now());
        Task saved = service.create(draft);

        assertThat(saved.id()).isEqualTo("id-1");
        assertThat(saved.createdAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void get_filters_by_owner() {
        when(repository.findById("t1")).thenReturn(Optional.of(new Task("t1", "u1", "d", Priority.LOW, Status.OPEN, Instant.now(), Instant.now())));
        assertThat(service.get("t1", "u1")).isPresent();
        assertThat(service.get("t1", "u2")).isEmpty();
    }

    @Test
    void update_returns_null_when_not_owner_or_missing() {
        when(repository.findById("t1")).thenReturn(Optional.empty());
        Task upd = new Task(null, "u1", "x", Priority.MEDIUM, Status.OPEN, Instant.now(), Instant.now());
        assertThat(service.update("t1", "u1", upd)).isNull();
    }
}

