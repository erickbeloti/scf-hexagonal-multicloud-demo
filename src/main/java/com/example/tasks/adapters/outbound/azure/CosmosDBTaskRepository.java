package com.example.tasks.adapters.outbound.azure;

import com.azure.cosmos.models.PartitionKey;
import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Azure Cosmos DB implementation of TaskRepositoryPort.
 * Uses Spring Data Cosmos for persistence operations.
 */
@Repository
@Profile("azure")
public class CosmosDBTaskRepository implements TaskRepositoryPort {

    private final TaskDocumentDao repository;

    public CosmosDBTaskRepository(TaskDocumentDao repository) {
        this.repository = repository;
    }

    @Override
    public Task save(Task task) {
        try {
            TaskDocument document = TaskDocument.fromDomain(task);
            repository.save(document);
            return task;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save task to Cosmos DB", e);
        }
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        try {
            return repository.findById(id.value())
                .map(TaskDocument::toDomain);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find task in Cosmos DB", e);
        }
    }

    @Override
    public List<Task> findByUserId(UserId userId, int page, int size) {
        try {
            return repository.findByUserId(userId.value())
                .stream()
                .sorted(Comparator.comparing(TaskDocument::getCreatedAt).reversed())
                .skip((long) page * size)
                .limit(size)
                .map(TaskDocument::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to query tasks from Cosmos DB", e);
        }
    }

    @Override
    public long countHighPriorityTasksForUserOnDate(UserId userId, LocalDate date) {
        try {
            String startDate = date.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String endDate = date.plusDays(1).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            return repository.findByUserIdAndPriorityAndCreatedAtBetween(
                userId.value(), "HIGH", startDate, endDate).size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count high priority tasks", e);
        }
    }

    @Override
    public boolean existsByUserAndDateAndDescription(UserId userId, LocalDate date, String description) {
        try {
            String startDate = date.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String endDate = date.plusDays(1).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            return !repository.findByUserIdAndDescriptionAndCreatedAtBetween(
                userId.value(), description, startDate, endDate).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to check task existence", e);
        }
    }

    @Override
    public long countOpenTasksForUser(UserId userId) {
        try {
            return repository.findByUserIdAndStatus(userId.value(), "OPEN").size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count open tasks", e);
        }
    }

    @Override
    public void deleteById(TaskId id) {
        try {
            // First find the document to get the userId (partition key)
            repository.findById(id.value()).ifPresent(doc ->
                repository.deleteById(id.value(), new PartitionKey(doc.getUserId()))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete task from Cosmos DB", e);
        }
    }
}

