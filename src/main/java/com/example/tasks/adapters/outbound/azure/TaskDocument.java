package com.example.tasks.adapters.outbound.azure;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import com.example.tasks.domain.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cosmos DB document representation of a Task.
 * Uses userId as partition key for optimized queries.
 */
@Container(containerName = "tasks")
public class TaskDocument {

    @Id
    private String id;

    @PartitionKey
    private String userId;

    private String description;
    private String priority;
    private String status;
    private String createdAt;
    private String updatedAt;

    public TaskDocument() {}

    // Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Creates a TaskDocument from a domain Task.
     */
    public static TaskDocument fromDomain(Task task) {
        TaskDocument doc = new TaskDocument();
        doc.setId(task.getId().value());
        doc.setUserId(task.getUserId().value());
        doc.setDescription(task.getDescription());
        doc.setPriority(task.getPriority().name());
        doc.setStatus(task.getStatus().name());
        doc.setCreatedAt(task.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        doc.setUpdatedAt(task.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return doc;
    }

    /**
     * Converts this document back to a domain Task.
     */
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

