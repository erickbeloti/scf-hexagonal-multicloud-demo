package com.example.tasks.adapters.outbound.aws;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.model.Priority;
import com.example.tasks.domain.model.Status;
import com.example.tasks.domain.model.Task;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Profile("aws")
public class DynamoDbTaskRepository implements TaskRepositoryPort {
    private final DynamoDbClient client;
    private final String table = "Tasks";

    public DynamoDbTaskRepository() {
        this.client = DynamoDbClient.builder().region(Region.of(Optional.ofNullable(System.getenv("AWS_REGION")).orElse("us-east-1"))).build();
    }

    @Override
    public Task save(Task task) {
        Map<String, AttributeValue> item = toItem(task);
        client.putItem(PutItemRequest.builder().tableName(table).item(item).build());
        return task;
    }

    @Override
    public Optional<Task> findById(String id) {
        Map<String, AttributeValue> key = Map.of("id", AttributeValue.builder().s(id).build());
        GetItemResponse res = client.getItem(GetItemRequest.builder().tableName(table).key(key).build());
        if (res.hasItem()) return Optional.of(fromItem(res.item()));
        return Optional.empty();
    }

    @Override
    public Optional<Task> findByUserAndDateAndDescription(String userId, LocalDate date, String description) {
        String gsi = "GSI1"; // userId#date partition, description sort key recommended
        String pk = userId + "#" + date.toString();
        QueryRequest req = QueryRequest.builder()
                .tableName(table)
                .indexName(gsi)
                .keyConditionExpression("pk = :pk and begins_with(sk, :desc)")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.builder().s(pk).build(),
                        ":desc", AttributeValue.builder().s(description).build()
                ))
                .limit(1).build();
        QueryResponse resp = client.query(req);
        if (resp.count() > 0) return Optional.of(fromItem(resp.items().get(0)));
        return Optional.empty();
    }

    @Override
    public long countHighPriorityForUserOn(LocalDate date, String userId) {
        String gsi = "GSI1";
        String pk = userId + "#" + date.toString();
        QueryResponse resp = client.query(QueryRequest.builder()
                .tableName(table)
                .indexName(gsi)
                .keyConditionExpression("pk = :pk")
                .filterExpression("priority = :high")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.builder().s(pk).build(),
                        ":high", AttributeValue.builder().s(Priority.HIGH.name()).build()
                )).build());
        return resp.count();
    }

    @Override
    public long countOpenByUser(String userId) {
        String gsi = "GSI2"; // partition by userId for listing
        QueryResponse resp = client.query(QueryRequest.builder()
                .tableName(table)
                .indexName(gsi)
                .keyConditionExpression("userId = :u")
                .filterExpression("status <> :completed")
                .expressionAttributeValues(Map.of(
                        ":u", AttributeValue.builder().s(userId).build(),
                        ":completed", AttributeValue.builder().s(Status.COMPLETED.name()).build()
                )).build());
        return resp.count();
    }

    @Override
    public List<Task> listByUser(String userId, int page, int size) {
        String gsi = "GSI2";
        QueryResponse resp = client.query(QueryRequest.builder()
                .tableName(table)
                .indexName(gsi)
                .keyConditionExpression("userId = :u")
                .expressionAttributeValues(Map.of(":u", AttributeValue.builder().s(userId).build()))
                .limit(size)
                .build());
        return resp.items().stream().map(this::fromItem).collect(Collectors.toList());
    }

    @Override
    public boolean deleteByIdAndUser(String id, String userId) {
        Optional<Task> t = findById(id);
        if (t.isPresent() && t.get().userId().equals(userId)) {
            client.deleteItem(DeleteItemRequest.builder().tableName(table)
                    .key(Map.of("id", AttributeValue.builder().s(id).build())).build());
            return true;
        }
        return false;
    }

    private Map<String, AttributeValue> toItem(Task t) {
        LocalDate day = LocalDate.ofInstant(t.createdAt(), ZoneOffset.UTC);
        Map<String, AttributeValue> m = new HashMap<>();
        m.put("id", AttributeValue.builder().s(t.id()).build());
        m.put("userId", AttributeValue.builder().s(t.userId()).build());
        m.put("description", AttributeValue.builder().s(t.description()).build());
        m.put("priority", AttributeValue.builder().s(t.priority().name()).build());
        m.put("status", AttributeValue.builder().s(t.status().name()).build());
        m.put("createdAt", AttributeValue.builder().s(t.createdAt().toString()).build());
        m.put("updatedAt", AttributeValue.builder().s(t.updatedAt().toString()).build());
        m.put("pk", AttributeValue.builder().s(t.userId() + "#" + day).build());
        m.put("sk", AttributeValue.builder().s(t.description()).build());
        return m;
    }

    private Task fromItem(Map<String, AttributeValue> i) {
        return new Task(
                i.get("id").s(),
                i.get("userId").s(),
                i.get("description").s(),
                Priority.valueOf(i.get("priority").s()),
                Status.valueOf(i.get("status").s()),
                Instant.parse(i.get("createdAt").s()),
                Instant.parse(i.get("updatedAt").s())
        );
    }
}

