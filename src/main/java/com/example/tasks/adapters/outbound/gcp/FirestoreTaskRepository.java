package com.example.tasks.adapters.outbound.gcp;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.model.Priority;
import com.example.tasks.domain.model.Status;
import com.example.tasks.domain.model.Task;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
@Profile("gcp")
public class FirestoreTaskRepository implements TaskRepositoryPort {
    private final Firestore db = FirestoreOptions.getDefaultInstance().getService();
    private final CollectionReference col = db.collection("tasks");

    @Override
    public Task save(Task task) {
        Map<String, Object> data = toMap(task);
        col.document(task.id()).set(data);
        return task;
    }

    @Override
    public Optional<Task> findById(String id) {
        try {
            DocumentSnapshot snap = col.document(id).get().get();
            if (snap.exists()) return Optional.of(fromDoc(snap));
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Task> findByUserAndDateAndDescription(String userId, LocalDate date, String description) {
        try {
            Query q = col.whereEqualTo("userId", userId)
                    .whereEqualTo("description", description)
                    .whereGreaterThanOrEqualTo("createdAt", date.atStartOfDay().toInstant(ZoneOffset.UTC))
                    .whereLessThan("createdAt", date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
            List<QueryDocumentSnapshot> docs = q.get().get().getDocuments();
            if (docs.isEmpty()) return Optional.empty();
            return Optional.of(fromDoc(docs.get(0)));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public long countHighPriorityForUserOn(LocalDate date, String userId) {
        try {
            Query q = col.whereEqualTo("userId", userId)
                    .whereEqualTo("priority", Priority.HIGH.name())
                    .whereGreaterThanOrEqualTo("createdAt", date.atStartOfDay().toInstant(ZoneOffset.UTC))
                    .whereLessThan("createdAt", date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
            return q.get().get().size();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public long countOpenByUser(String userId) {
        try {
            Query q = col.whereEqualTo("userId", userId).whereNotEqualTo("status", Status.COMPLETED.name());
            return q.get().get().size();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Task> listByUser(String userId, int page, int size) {
        try {
            Query q = col.whereEqualTo("userId", userId).orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(size).offset(page * size);
            return q.get().get().getDocuments().stream().map(this::fromDoc).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteByIdAndUser(String id, String userId) {
        Optional<Task> t = findById(id);
        if (t.isPresent() && t.get().userId().equals(userId)) {
            col.document(id).delete();
            return true;
        }
        return false;
    }

    private Map<String, Object> toMap(Task t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.id());
        m.put("userId", t.userId());
        m.put("description", t.description());
        m.put("priority", t.priority().name());
        m.put("status", t.status().name());
        m.put("createdAt", t.createdAt());
        m.put("updatedAt", t.updatedAt());
        return m;
    }

    private Task fromDoc(DocumentSnapshot d) {
        return new Task(
                d.getString("id"),
                d.getString("userId"),
                d.getString("description"),
                Priority.valueOf(d.getString("priority")),
                Status.valueOf(d.getString("status")),
                d.getTimestamp("createdAt").toDate().toInstant(),
                d.getTimestamp("updatedAt").toDate().toInstant()
        );
    }
}

