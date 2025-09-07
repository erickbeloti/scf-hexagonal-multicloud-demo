package com.example.tasks.adapters.outbound.gcp;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.Priority;
import com.example.tasks.domain.Status;
import com.example.tasks.domain.Task;
import com.example.tasks.domain.TaskId;
import com.example.tasks.domain.UserId;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
@Profile("gcp")
public class FirestoreTaskRepository implements TaskRepositoryPort {

    private final Firestore firestore;
    private static final String COLLECTION = "tasks";

    public FirestoreTaskRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Task save(Task task) {
        try {
            Map<String, Object> taskData = Map.of(
                "id", task.getId().value(),
                "userId", task.getUserId().value(),
                "description", task.getDescription(),
                "priority", task.getPriority().name(),
                "status", task.getStatus().name(),
                "createdAt", task.getCreatedAt().toString(),
                "updatedAt", task.getUpdatedAt().toString()
            );

            firestore.collection(COLLECTION).document(task.getId().value()).set(taskData).get();
            return task;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to save task", e);
        }
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        try {
            DocumentSnapshot document = firestore.collection(COLLECTION)
                .document(id.value()).get().get();

            if (!document.exists()) {
                return Optional.empty();
            }

            return Optional.of(mapToTask(document));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to find task", e);
        }
    }

    @Override
    public List<Task> findByUserId(UserId userId, int page, int size) {
        try {
            Query query = firestore.collection(COLLECTION)
                .whereEqualTo("userId", userId.value())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .offset(page * size)
                .limit(size);

            return query.get().get().getDocuments().stream()
                .map(this::mapToTask)
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to find tasks", e);
        }
    }

    @Override
    public void deleteById(TaskId id) {
        try {
            firestore.collection(COLLECTION).document(id.value()).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to delete task", e);
        }
    }

    @Override
    public boolean existsByUserAndDateAndDescription(UserId userId, LocalDate date, String description) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            Query query = firestore.collection(COLLECTION)
                .whereEqualTo("userId", userId.value())
                .whereEqualTo("description", description)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay.toString())
                .whereLessThanOrEqualTo("createdAt", endOfDay.toString());

            return !query.get().get().isEmpty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to check description uniqueness", e);
        }
    }

    @Override
    public long countHighPriorityTasksForUserOnDate(UserId userId, LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            Query query = firestore.collection(COLLECTION)
                .whereEqualTo("userId", userId.value())
                .whereEqualTo("priority", "HIGH")
                .whereGreaterThanOrEqualTo("createdAt", startOfDay.toString())
                .whereLessThanOrEqualTo("createdAt", endOfDay.toString());

            return query.get().get().size();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to count high priority tasks", e);
        }
    }

    @Override
    public long countOpenTasksForUser(UserId userId) {
        try {
            Query query = firestore.collection(COLLECTION)
                .whereEqualTo("userId", userId.value())
                .whereEqualTo("status", "OPEN");

            return query.get().get().size();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to count open tasks", e);
        }
    }

    private Task mapToTask(DocumentSnapshot document) {
        return Task.reconstitute(
            TaskId.of(document.getString("id")),
            UserId.of(document.getString("userId")),
            document.getString("description"),
            Priority.valueOf(document.getString("priority")),
            Status.valueOf(document.getString("status")),
            LocalDateTime.parse(document.getString("createdAt")),
            LocalDateTime.parse(document.getString("updatedAt"))
        );
    }
}
