package com.example.tasks.adapters.out.firestore;

import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.port.out.TaskRepository;
import com.example.tasks.config.Profiles;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
@Profile(Profiles.GCP)
public class FirestoreTaskRepository implements TaskRepository {

    private final Firestore firestore;

    public FirestoreTaskRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection() {
        return firestore.collection("tasks");
    }

    @Override
    public Task createTask(Task task) {
        Map<String, Object> data = Map.of(
                "title", task.title(),
                "description", task.description()
        );
        collection().document(task.id()).set(data);
        return task;
    }

    @Override
    public Optional<Task> getTask(String id) {
        try {
            DocumentSnapshot doc = collection().document(id).get().get();
            if (!doc.exists()) {
                return Optional.empty();
            }
            return Optional.of(new Task(doc.getId(), doc.getString("title"), doc.getString("description")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Task updateTask(Task task) {
        Map<String, Object> data = Map.of(
                "title", task.title(),
                "description", task.description()
        );
        collection().document(task.id()).set(data);
        return task;
    }

    @Override
    public void deleteTask(String id) {
        collection().document(id).delete();
    }
}
