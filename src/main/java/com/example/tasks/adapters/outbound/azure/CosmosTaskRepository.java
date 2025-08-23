package com.example.tasks.adapters.outbound.azure;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.Task;
import com.example.tasks.domain.Priority;
import com.example.tasks.domain.Status;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
@Profile("azure")
public class CosmosTaskRepository implements TaskRepositoryPort {
    
    private static final String DATABASE_NAME = "tasks-db";
    private static final String CONTAINER_NAME = "tasks";
    private static final String PARTITION_KEY = "/userId";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final CosmosClient cosmosClient;
    private CosmosDatabase database;
    private CosmosContainer container;
    
    @Value("${azure.cosmos.database-name:tasks-db}")
    private String databaseName;
    
    @Value("${azure.cosmos.container-name:tasks}")
    private String containerName;
    
    public CosmosTaskRepository(CosmosClient cosmosClient) {
        this.cosmosClient = cosmosClient;
    }
    
    @PostConstruct
    public void initialize() {
        createDatabaseIfNotExists();
        createContainerIfNotExists();
    }
    
    private void createDatabaseIfNotExists() {
        try {
            database = cosmosClient.getDatabase(databaseName);
            database.read();
        } catch (Exception e) {
            database = cosmosClient.createDatabaseIfNotExists(databaseName);
        }
    }
    
    private void createContainerIfNotExists() {
        try {
            container = database.getContainer(containerName);
            container.read();
        } catch (Exception e) {
            // Create container with composite indexes for efficient querying
            CosmosContainerProperties containerProperties = new CosmosContainerProperties(
                containerName, PARTITION_KEY);
            
            // Add composite indexes for efficient queries
            List<Index> indexes = Arrays.asList(
                // Index for user-date queries (daily tasks, uniqueness)
                new CompositeIndex(Arrays.asList(
                    new IndexPath("/userId", IndexType.RANGE),
                    new IndexPath("/date", IndexType.RANGE),
                    new IndexPath("/description", IndexType.RANGE)
                )),
                // Index for user-status queries (counting open tasks)
                new CompositeIndex(Arrays.asList(
                    new IndexPath("/userId", IndexType.RANGE),
                    new IndexPath("/status", IndexType.RANGE)
                )),
                // Index for user-priority-date queries (high priority counting)
                new CompositeIndex(Arrays.asList(
                    new IndexPath("/userId", IndexType.RANGE),
                    new IndexPath("/date", IndexType.RANGE),
                    new IndexPath("/priority", IndexType.RANGE)
                ))
            );
            
            containerProperties.setIndexingPolicy(new IndexingPolicy(indexes));
            
            container = database.createContainerIfNotExists(containerProperties);
        }
    }
    
    @Override
    public Task save(Task task) {
        TaskDocument doc = new TaskDocument(task);
        container.createItem(doc, new PartitionKey(task.userId()), null);
        return task;
    }
    
    @Override
    public Optional<Task> findById(String id) {
        try {
            // We need to query by id since we don't know the partition key
            String sql = "SELECT * FROM c WHERE c.id = @id";
            SqlParameter param = new SqlParameter("@id", id);
            SqlQuerySpec querySpec = new SqlQuerySpec(sql, Arrays.asList(param));
            
            CosmosPagedIterable<TaskDocument> results = container.queryItems(querySpec, 
                new CosmosQueryRequestOptions(), TaskDocument.class);
            
            return StreamSupport.stream(results.spliterator(), false)
                .findFirst()
                .map(TaskDocument::toTask);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<Task> findByUserAndDateAndDescription(String userId, LocalDate date, String description) {
        String sql = "SELECT * FROM c WHERE c.userId = @userId AND c.date = @date AND c.description = @description";
        List<SqlParameter> params = Arrays.asList(
            new SqlParameter("@userId", userId),
            new SqlParameter("@date", date.format(DATE_FORMATTER)),
            new SqlParameter("@description", description)
        );
        
        SqlQuerySpec querySpec = new SqlQuerySpec(sql, params);
        CosmosPagedIterable<TaskDocument> results = container.queryItems(querySpec, 
            new CosmosQueryRequestOptions(), TaskDocument.class);
        
        return StreamSupport.stream(results.spliterator(), false)
            .findFirst()
            .map(TaskDocument::toTask);
    }
    
    @Override
    public long countHighPriorityForUserOn(LocalDate date, String userId) {
        String sql = "SELECT VALUE COUNT(1) FROM c WHERE c.userId = @userId AND c.date = @date AND c.priority = @priority";
        List<SqlParameter> params = Arrays.asList(
            new SqlParameter("@userId", userId),
            new SqlParameter("@date", date.format(DATE_FORMATTER)),
            new SqlParameter("@priority", Priority.HIGH.name())
        );
        
        SqlQuerySpec querySpec = new SqlQuerySpec(sql, params);
        CosmosPagedIterable<Integer> results = container.queryItems(querySpec, 
            new CosmosQueryRequestOptions(), Integer.class);
        
        return StreamSupport.stream(results.spliterator(), false)
            .findFirst()
            .orElse(0);
    }
    
    @Override
    public long countOpenByUser(String userId) {
        String sql = "SELECT VALUE COUNT(1) FROM c WHERE c.userId = @userId AND c.status != @completed";
        List<SqlParameter> params = Arrays.asList(
            new SqlParameter("@userId", userId),
            new SqlParameter("@completed", Status.COMPLETED.name())
        );
        
        SqlQuerySpec querySpec = new SqlQuerySpec(sql, params);
        CosmosPagedIterable<Integer> results = container.queryItems(querySpec, 
            new CosmosQueryRequestOptions(), Integer.class);
        
        return StreamSupport.stream(results.spliterator(), false)
            .findFirst()
            .orElse(0);
    }
    
    @Override
    public List<Task> listByUser(String userId, int page, int size) {
        String sql = "SELECT * FROM c WHERE c.userId = @userId ORDER BY c.createdAt DESC OFFSET @offset LIMIT @limit";
        List<SqlParameter> params = Arrays.asList(
            new SqlParameter("@userId", userId),
            new SqlParameter("@offset", page * size),
            new SqlParameter("@limit", size)
        );
        
        SqlQuerySpec querySpec = new SqlQuerySpec(sql, params);
        CosmosPagedIterable<TaskDocument> results = container.queryItems(querySpec, 
            new CosmosQueryRequestOptions(), TaskDocument.class);
        
        return StreamSupport.stream(results.spliterator(), false)
            .map(TaskDocument::toTask)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteByIdAndUser(String id, String userId) {
        try {
            // First find the item to get its etag for optimistic concurrency
            Optional<Task> task = findById(id);
            if (task.isPresent() && task.get().userId().equals(userId)) {
                String sql = "SELECT c._etag FROM c WHERE c.id = @id AND c.userId = @userId";
                List<SqlParameter> params = Arrays.asList(
                    new SqlParameter("@id", id),
                    new SqlParameter("@userId", userId)
                );
                
                SqlQuerySpec querySpec = new SqlQuerySpec(sql, params);
                CosmosPagedIterable<Map<String, String>> results = container.queryItems(querySpec, 
                    new CosmosQueryRequestOptions(), Map.class);
                
                Map<String, String> item = StreamSupport.stream(results.spliterator(), false)
                    .findFirst()
                    .orElse(null);
                
                if (item != null) {
                    container.deleteItem(id, new PartitionKey(userId), 
                        new CosmosItemRequestOptions().setIfMatchEtag(item.get("_etag")));
                }
            }
        } catch (Exception e) {
            // Log error but don't throw
        }
    }
    
    // Document class for Cosmos DB
    private static class TaskDocument {
        private String id;
        private String userId;
        private String description;
        private String priority;
        private String status;
        private String date;
        private String createdAt;
        private String updatedAt;
        
        public TaskDocument() {}
        
        public TaskDocument(Task task) {
            this.id = task.id();
            this.userId = task.userId();
            this.description = task.description();
            this.priority = task.priority().name();
            this.status = task.status().name();
            this.date = task.getDate().format(DATE_FORMATTER);
            this.createdAt = task.createdAt().toString();
            this.updatedAt = task.updatedAt().toString();
        }
        
        public Task toTask() {
            return new Task(
                id, userId, description,
                Priority.valueOf(priority),
                Status.valueOf(status),
                LocalDateTime.parse(createdAt),
                LocalDateTime.parse(updatedAt)
            );
        }
        
        // Getters and setters for JSON serialization
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }
}