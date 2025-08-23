package com.example.tasks.adapters.outbound.azure;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.CosmosStoredProcedureRequestOptions;
import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.model.Priority;
import com.example.tasks.domain.model.Status;
import com.example.tasks.domain.model.Task;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Profile("azure")
public class CosmosTaskRepository implements TaskRepositoryPort {
    private final CosmosClient client;
    private final CosmosContainer container;

    public CosmosTaskRepository() {
        String endpoint = System.getenv().getOrDefault("AZURE_COSMOS_ENDPOINT", "http://localhost:8081");
        String key = System.getenv().getOrDefault("AZURE_COSMOS_KEY", "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9Pbyz1m==");
        String dbName = System.getenv().getOrDefault("AZURE_COSMOS_DB", "tasksdb");
        String containerName = System.getenv().getOrDefault("AZURE_COSMOS_CONTAINER", "tasks");
        this.client = new CosmosClientBuilder().endpoint(endpoint).key(key).consistencyLevel(ConsistencyLevel.EVENTUAL).buildClient();
        this.container = client.getDatabase(dbName).getContainer(containerName);
    }

    @Override
    public Task save(Task task) {
        container.upsertItem(toDoc(task));
        return task;
    }

    @Override
    public Optional<Task> findById(String id) {
        try {
            var response = container.readItem(id, new PartitionKey("user:"), Map.class);
            return Optional.of(fromDoc(response.getItem()));
        } catch (CosmosException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Task> findByUserAndDateAndDescription(String userId, LocalDate date, String description) {
        String sql = "SELECT TOP 1 * FROM c WHERE c.userId = @userId AND c.description = @desc AND c.createdAt >= @start AND c.createdAt < @end";
        var params = new SqlParameterList(
                new SqlParameter("@userId", userId),
                new SqlParameter("@desc", description),
                new SqlParameter("@start", date.atStartOfDay().toInstant(ZoneOffset.UTC).toString()),
                new SqlParameter("@end", date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString())
        );
        var it = container.queryItems(new SqlQuerySpec(sql, params), new CosmosQueryRequestOptions(), Map.class).stream();
        return it.findFirst().map(this::fromDoc);
    }

    @Override
    public long countHighPriorityForUserOn(LocalDate date, String userId) {
        String sql = "SELECT VALUE COUNT(1) FROM c WHERE c.userId = @userId AND c.priority = 'HIGH' AND c.createdAt >= @start AND c.createdAt < @end";
        var params = new SqlParameterList(
                new SqlParameter("@userId", userId),
                new SqlParameter("@start", date.atStartOfDay().toInstant(ZoneOffset.UTC).toString()),
                new SqlParameter("@end", date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString())
        );
        var it = container.queryItems(new SqlQuerySpec(sql, params), new CosmosQueryRequestOptions(), Integer.class).stream();
        return it.findFirst().orElse(0);
    }

    @Override
    public long countOpenByUser(String userId) {
        String sql = "SELECT VALUE COUNT(1) FROM c WHERE c.userId = @userId AND c.status != 'COMPLETED'";
        var params = new SqlParameterList(new SqlParameter("@userId", userId));
        var it = container.queryItems(new SqlQuerySpec(sql, params), new CosmosQueryRequestOptions(), Integer.class).stream();
        return it.findFirst().orElse(0);
    }

    @Override
    public List<Task> listByUser(String userId, int page, int size) {
        String sql = "SELECT * FROM c WHERE c.userId = @userId ORDER BY c.createdAt DESC OFFSET @off LIMIT @lim";
        var params = new SqlParameterList(
                new SqlParameter("@userId", userId),
                new SqlParameter("@off", page * size),
                new SqlParameter("@lim", size)
        );
        return container.queryItems(new SqlQuerySpec(sql, params), new CosmosQueryRequestOptions(), Map.class)
                .stream().map(this::fromDoc).collect(Collectors.toList());
    }

    @Override
    public boolean deleteByIdAndUser(String id, String userId) {
        try {
            container.deleteItem(id, new PartitionKey("user:"));
            return true;
        } catch (CosmosException e) {
            return false;
        }
    }

    private Map<String, Object> toDoc(Task t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.id());
        m.put("userId", t.userId());
        m.put("description", t.description());
        m.put("priority", t.priority().name());
        m.put("status", t.status().name());
        m.put("createdAt", t.createdAt().toString());
        m.put("updatedAt", t.updatedAt().toString());
        return m;
    }

    private Task fromDoc(Map<String, Object> d) {
        return new Task(
                (String) d.get("id"),
                (String) d.get("userId"),
                (String) d.get("description"),
                Priority.valueOf((String) d.get("priority")),
                Status.valueOf((String) d.get("status")),
                Instant.parse((String) d.get("createdAt")),
                Instant.parse((String) d.get("updatedAt"))
        );
    }
}

