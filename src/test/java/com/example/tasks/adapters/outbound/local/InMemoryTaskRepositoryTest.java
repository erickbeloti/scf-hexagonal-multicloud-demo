package com.example.tasks.adapters.outbound.local;

import com.example.tasks.domain.model.Priority;
import com.example.tasks.domain.model.Status;
import com.example.tasks.domain.model.Task;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryTaskRepositoryTest {
    @Test
    void saves_and_reads() {
        InMemoryTaskRepository repo = new InMemoryTaskRepository();
        Task t = new Task("1", "u1", "d1", Priority.LOW, Status.OPEN, Instant.now(), Instant.now());
        repo.save(t);
        assertThat(repo.findById("1")).contains(t);
    }
}

