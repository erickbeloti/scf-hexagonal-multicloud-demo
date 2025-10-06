package com.example.tasks.adapters.outbound.aws;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.Task;
import com.example.tasks.domain.TaskId;
import com.example.tasks.domain.UserId;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Profile("aws")
public class DynamoDBTaskRepository implements TaskRepositoryPort {

    private final DynamoDbTemplate dynamoDbTemplate;
    private final DynamoDbTable<TaskEntity> taskTable;

    public DynamoDBTaskRepository(DynamoDbTemplate dynamoDbTemplate, DynamoDbTable<TaskEntity> taskTable) {
        this.dynamoDbTemplate = dynamoDbTemplate;
        this.taskTable = taskTable;
    }

    @Override
    public Task save(Task task) {
        try {
            TaskEntity entity = TaskEntity.fromDomain(task);
            taskTable.putItem(entity);
            return task;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save task to DynamoDB", e);
        }
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        try {
            Key key = Key.builder().partitionValue(id.value()).build();
            TaskEntity entity = taskTable.getItem(key);
            return entity != null ?
                Optional.of(entity.toDomain()) :
                Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find task in DynamoDB", e);
        }
    }

    @Override
    public List<Task> findByUserId(UserId userId, int page, int size) {
        try {
            return taskTable.scan()
                .items()
                .stream()
                .filter(entity -> userId.value().equals(entity.getUserId()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .skip((long) page * size)
                .limit(size)
                .map(TaskEntity::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to query tasks from DynamoDB", e);
        }
    }

    @Override
    public long countHighPriorityTasksForUserOnDate(UserId userId, LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            return taskTable.scan()
                .items()
                .stream()
                .filter(entity -> userId.value().equals(entity.getUserId()))
                .filter(entity -> "HIGH".equals(entity.getPriority()))
                .filter(entity -> {
                    LocalDateTime createdAt = LocalDateTime.parse(entity.getCreatedAt());
                    return createdAt.isAfter(startOfDay) && createdAt.isBefore(endOfDay);
                })
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count high priority tasks", e);
        }
    }

    @Override
    public boolean existsByUserAndDateAndDescription(UserId userId, LocalDate date, String description) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            return taskTable.scan()
                .items()
                .stream()
                .filter(entity -> userId.value().equals(entity.getUserId()))
                .filter(entity -> description.equals(entity.getDescription()))
                .anyMatch(entity -> {
                    LocalDateTime createdAt = LocalDateTime.parse(entity.getCreatedAt());
                    return createdAt.isAfter(startOfDay) && createdAt.isBefore(endOfDay);
                });
        } catch (Exception e) {
            throw new RuntimeException("Failed to check task existence", e);
        }
    }

    @Override
    public long countOpenTasksForUser(UserId userId) {
        try {
            return taskTable.scan()
                .items()
                .stream()
                .filter(entity -> userId.value().equals(entity.getUserId()))
                .filter(entity -> "OPEN".equals(entity.getStatus()))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count open tasks", e);
        }
    }

    @Override
    public void deleteById(TaskId id) {
        try {
            Key key = Key.builder().partitionValue(id.value()).build();
            taskTable.deleteItem(key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete task from DynamoDB", e);
        }
    }
}
