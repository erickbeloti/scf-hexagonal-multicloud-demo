package com.example.tasks.adapters.outbound.gcp;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;

@Repository
@Profile("gcp")
public class FirestoreTaskRepository implements TaskRepositoryPort {

    private final FirestoreTemplate template;

    public FirestoreTaskRepository(FirestoreTemplate template) {
        this.template = template;
    }

    @Override
    public Task save(Task task) {
        return template.save(task).block();
    }

    @Override
    public Optional<Task> findById(String id) {
        return template.findById(id, Task.class).blockOptional();
    }

    @Override
    public Optional<Task> findByUserAndDateAndDescription(String userId, LocalDate date, String description) {
        return template.findAll(Task.class)
            .filter(t -> t.userId().equals(userId)
                    && t.description().equals(description)
                    && t.createdAt().toLocalDate().equals(date))
            .blockFirstOptional();
    }

    @Override
    public long countHighPriorityForUserOn(String userId, LocalDate date) {
        return template.findAll(Task.class)
            .filter(t -> t.userId().equals(userId)
                    && t.priority() == Priority.HIGH
                    && t.createdAt().toLocalDate().equals(date))
            .count()
            .blockOptional()
            .orElse(0L);
    }

    @Override
    public long countOpenByUser(String userId) {
        return template.findAll(Task.class)
            .filter(t -> t.userId().equals(userId)
                    && t.status() == Status.OPEN)
            .count()
            .blockOptional()
            .orElse(0L);
    }

    @Override
    public List<Task> listByUser(String userId, int page, int size) {
        return template.findAll(Task.class)
            .filter(t -> t.userId().equals(userId))
            .skip((long) page * size)
            .take(size)
            .collectList()
            .block();
    }

    @Override
    public void deleteByIdAndUser(String id, String userId) {
        template.findAll(Task.class)
            .filter(t -> t.id().equals(id) && t.userId().equals(userId))
            .flatMap(template::delete)
            .blockLast();
    }
}
