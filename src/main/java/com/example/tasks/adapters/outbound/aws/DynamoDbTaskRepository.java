package com.example.tasks.adapters.outbound.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
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

@Repository
@Profile("aws")
public class DynamoDbTaskRepository implements TaskRepositoryPort {
    
    private static final String TABLE_NAME = "tasks";
    private static final String USER_DATE_INDEX = "user-date-index";
    private static final String USER_STATUS_INDEX = "user-status-index";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final AmazonDynamoDB dynamoDB;
    private final DynamoDB dynamoDBDocument;
    
    @Value("${aws.dynamodb.table-name:tasks}")
    private String tableName;
    
    public DynamoDbTaskRepository(AmazonDynamoDB dynamoDB) {
        this.dynamoDB = dynamoDB;
        this.dynamoDBDocument = new DynamoDB(dynamoDB);
    }
    
    @PostConstruct
    public void createTableIfNotExists() {
        try {
            Table table = dynamoDBDocument.getTable(tableName);
            table.describe();
        } catch (Exception e) {
            createTable();
        }
    }
    
    private void createTable() {
        CreateTableRequest request = new CreateTableRequest()
            .withTableName(tableName)
            .withKeySchema(
                new KeySchemaElement("id", KeyType.HASH)
            )
            .withAttributeDefinitions(
                new AttributeDefinition("id", ScalarAttributeType.S),
                new AttributeDefinition("userId", ScalarAttributeType.S),
                new AttributeDefinition("date", ScalarAttributeType.S),
                new AttributeDefinition("status", ScalarAttributeType.S)
            )
            .withProvisionedThroughput(
                new ProvisionedThroughput(5L, 5L)
            )
            .withGlobalSecondaryIndexes(
                // User-Date index for daily queries and uniqueness
                new GlobalSecondaryIndex()
                    .withIndexName(USER_DATE_INDEX)
                    .withKeySchema(
                        new KeySchemaElement("userId", KeyType.HASH),
                        new KeySchemaElement("date", KeyType.RANGE)
                    )
                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L)),
                
                // User-Status index for counting open tasks
                new GlobalSecondaryIndex()
                    .withIndexName(USER_STATUS_INDEX)
                    .withKeySchema(
                        new KeySchemaElement("userId", KeyType.HASH),
                        new KeySchemaElement("status", KeyType.RANGE)
                    )
                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
            );
        
        dynamoDB.createTable(request);
    }
    
    @Override
    public Task save(Task task) {
        Table table = dynamoDBDocument.getTable(tableName);
        
        Item item = new Item()
            .withPrimaryKey("id", task.id())
            .withString("userId", task.userId())
            .withString("description", task.description())
            .withString("priority", task.priority().name())
            .withString("status", task.status().name())
            .withString("date", task.getDate().format(DATE_FORMATTER))
            .withString("createdAt", task.createdAt().toString())
            .withString("updatedAt", task.updatedAt().toString());
        
        table.putItem(item);
        return task;
    }
    
    @Override
    public Optional<Task> findById(String id) {
        Table table = dynamoDBDocument.getTable(tableName);
        Item item = table.getItem("id", id);
        return Optional.ofNullable(item).map(this::itemToTask);
    }
    
    @Override
    public Optional<Task> findByUserAndDateAndDescription(String userId, LocalDate date, String description) {
        Table table = dynamoDBDocument.getTable(tableName);
        Index index = table.getIndex(USER_DATE_INDEX);
        
        QuerySpec spec = new QuerySpec()
            .withHashKey("userId", userId)
            .withRangeKeyCondition(new RangeKeyCondition("date").eq(date.format(DATE_FORMATTER)))
            .withFilterExpression("description = :desc")
            .withValueMap(new ValueMap().withString(":desc", description));
        
        ItemCollection<QueryOutcome> items = index.query(spec);
        return items.iterator().hasNext() ? 
            Optional.of(itemToTask(items.iterator().next())) : Optional.empty();
    }
    
    @Override
    public long countHighPriorityForUserOn(LocalDate date, String userId) {
        Table table = dynamoDBDocument.getTable(tableName);
        Index index = table.getIndex(USER_DATE_INDEX);
        
        QuerySpec spec = new QuerySpec()
            .withHashKey("userId", userId)
            .withRangeKeyCondition(new RangeKeyCondition("date").eq(date.format(DATE_FORMATTER)))
            .withFilterExpression("priority = :priority")
            .withValueMap(new ValueMap().withString(":priority", Priority.HIGH.name()));
        
        ItemCollection<QueryOutcome> items = index.query(spec);
        long count = 0;
        for (Item item : items) {
            count++;
        }
        return count;
    }
    
    @Override
    public long countOpenByUser(String userId) {
        Table table = dynamoDBDocument.getTable(tableName);
        Index index = table.getIndex(USER_STATUS_INDEX);
        
        QuerySpec spec = new QuerySpec()
            .withHashKey("userId", userId)
            .withFilterExpression("status <> :completed")
            .withValueMap(new ValueMap().withString(":completed", Status.COMPLETED.name()));
        
        ItemCollection<QueryOutcome> items = index.query(spec);
        long count = 0;
        for (Item item : items) {
            count++;
        }
        return count;
    }
    
    @Override
    public List<Task> listByUser(String userId, int page, int size) {
        Table table = dynamoDBDocument.getTable(tableName);
        Index index = table.getIndex(USER_DATE_INDEX);
        
        QuerySpec spec = new QuerySpec()
            .withHashKey("userId", userId)
            .withScanIndexForward(false) // Most recent first
            .withMaxResultSize(page * size + size);
        
        ItemCollection<QueryOutcome> items = index.query(spec);
        List<Task> tasks = new ArrayList<>();
        int skip = page * size;
        int count = 0;
        
        for (Item item : items) {
            if (count >= skip && tasks.size() < size) {
                tasks.add(itemToTask(item));
            }
            count++;
        }
        
        return tasks;
    }
    
    @Override
    public void deleteByIdAndUser(String id, String userId) {
        Table table = dynamoDBDocument.getTable(tableName);
        table.deleteItem("id", id);
    }
    
    private Task itemToTask(Item item) {
        return new Task(
            item.getString("id"),
            item.getString("userId"),
            item.getString("description"),
            Priority.valueOf(item.getString("priority")),
            Status.valueOf(item.getString("status")),
            LocalDateTime.parse(item.getString("createdAt")),
            LocalDateTime.parse(item.getString("updatedAt"))
        );
    }
}