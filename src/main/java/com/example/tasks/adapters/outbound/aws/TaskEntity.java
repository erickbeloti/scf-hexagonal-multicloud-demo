package com.example.tasks.adapters.outbound.aws;

import com.example.tasks.domain.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@DynamoDbBean
public class TaskEntity {
    private String id;
    private String userId;
    private String description;
    private String priority;
    private String status;
    private String createdAt;
    private String updatedAt;

    public TaskEntity() {}

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "UserIdIndex")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @DynamoDbSecondarySortKey(indexNames = "UserIdIndex")
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static TaskEntity fromDomain(Task task) {
        TaskEntity entity = new TaskEntity();
        entity.setId(task.getId().value());
        entity.setUserId(task.getUserId().value());
        entity.setDescription(task.getDescription());
        entity.setPriority(task.getPriority().name());
        entity.setStatus(task.getStatus().name());
        entity.setCreatedAt(task.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        entity.setUpdatedAt(task.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return entity;
    }

    public Task toDomain() {
        return Task.reconstitute(
            TaskId.of(this.id),
            UserId.of(this.userId),
            this.description,
            Priority.valueOf(this.priority),
            Status.valueOf(this.status),
            LocalDateTime.parse(this.createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse(this.updatedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
