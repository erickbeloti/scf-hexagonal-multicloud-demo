package com.example.tasks.domain;

import java.time.LocalDateTime;

import org.springframework.cloud.gcp.data.firestore.Document;

@Document(collectionName = "tasks")
public record Task(
    String id,
    String userId,
    String description,
    Priority priority,
    Status status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
