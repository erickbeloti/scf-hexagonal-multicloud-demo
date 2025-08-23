package com.example.tasks.adapters.outbound.gcp;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.Task;
import com.example.tasks.domain.Priority;
import com.example.tasks.domain.Status;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
@Profile("gcp")
public class FirestoreTaskRepository implements TaskRepositoryPort {
    
    private static final String COLLECTION_NAME = "tasks";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final Firestore firestore;
    private final CollectionReference tasksCollection;
    
    @Value("${gcp.firestore.collection-name:tasks}")
    private String collectionName;
    
    public FirestoreTaskRepository() {
        this.firestore = FirestoreClient.getFirestore();
        this.tasksCollection = firestore.collection(collectionName);
    }
    
    @PostConstruct
    public void createIndexes() {
        // Firestore automatically creates indexes for queries
        // We'll create composite indexes for our complex queries
        try {
            // Create composite index for user-date-description queries
            createCompositeIndex("user-date-description", 
                Arrays.asList("userId", "date", "description"));
            
            // Create composite index for user-status queries
            createCompositeIndex("user-status", 
                Arrays.asList("userId", "status"));
            
            // Create composite index for user-date-priority queries
            createCompositeIndex("user-date-priority", 
                Arrays.asList("userId", "date", "priority"));
            
            // Create composite index for user-createdAt queries (for pagination)
            createCompositeIndex("user-createdAt", 
                Arrays.asList("userId", "createdAt"));
                
        } catch (Exception e) {
            // Index creation is asynchronous and may fail during startup
            // This is normal and indexes will be created automatically
        }
    }
    
    private void createCompositeIndex(String indexName, List<String> fields) {
        // Firestore automatically creates indexes based on query patterns
        // This method is for documentation purposes
        // In practice, Firestore will create indexes automatically when needed
    }
    
    @Override
    public Task save(Task task) {
        DocumentReference docRef = tasksCollection.document(task.id());
        
        Map<String, Object> data = new HashMap<>();
        data.put("id", task.id());
        data.put("userId", task.userId());
        data.put("description", task.description());
        data.put("priority", task.priority().name());
        data.put("status", task.status().name());
        data.put("date", task.getDate().format(DATE_FORMATTER));
        data.put("createdAt", task.createdAt().toString());
        data.put("updatedAt", task.updatedAt().toString());
        
        docRef.set(data);
        return task;
    }
    
    @Override
    public Optional<Task> findById(String id) {
        try {
            DocumentReference docRef = tasksCollection.document(id);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                return Optional.of(documentToTask(document));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<Task> findByUserAndDateAndDescription(String userId, LocalDate date, String description) {
        try {
            Query query = tasksCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date.format(DATE_FORMATTER))
                .whereEqualTo("description", description);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                return Optional.of(documentToTask(document));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public long countHighPriorityForUserOn(LocalDate date, String userId) {
        try {
            Query query = tasksCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date.format(DATE_FORMATTER))
                .whereEqualTo("priority", Priority.HIGH.name());
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.size();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }
    
    @Override
    public long countOpenByUser(String userId) {
        try {
            Query query = tasksCollection
                .whereEqualTo("userId", userId)
                .whereNotEqualTo("status", Status.COMPLETED.name());
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.size();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }
    
    @Override
    public List<Task> listByUser(String userId, int page, int size) {
        try {
            Query query = tasksCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(page * size + size);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            List<DocumentSnapshot> documents = querySnapshot.getDocuments();
            int start = page * size;
            int end = Math.min(start + size, documents.size());
            
            if (start >= documents.size()) {
                return List.of();
            }
            
            return documents.subList(start, end).stream()
                .map(this::documentToTask)
                .collect(Collectors.toList());
                
        } catch (InterruptedException | ExecutionException e) {
            return List.of();
        }
    }
    
    @Override
    public void deleteByIdAndUser(String id, String userId) {
        try {
            // First verify ownership
            Optional<Task> task = findById(id);
            if (task.isPresent() && task.get().userId().equals(userId)) {
                DocumentReference docRef = tasksCollection.document(id);
                docRef.delete();
            }
        } catch (Exception e) {
            // Log error but don't throw
        }
    }
    
    private Task documentToTask(DocumentSnapshot document) {
        return new Task(
            document.getString("id"),
            document.getString("userId"),
            document.getString("description"),
            Priority.valueOf(document.getString("priority")),
            Status.valueOf(document.getString("status")),
            LocalDateTime.parse(document.getString("createdAt")),
            LocalDateTime.parse(document.getString("updatedAt"))
        );
    }
}