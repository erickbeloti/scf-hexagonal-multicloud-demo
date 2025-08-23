package com.example.tasks.application.port.outbound;

import com.example.tasks.domain.Task;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepositoryPort {
    
    Task save(Task task);
    
    Optional<Task> findById(String id);
    
    Optional<Task> findByUserAndDateAndDescription(String userId, LocalDate date, String description);
    
    long countHighPriorityForUserOn(LocalDate date, String userId);
    
    long countOpenByUser(String userId);
    
    List<Task> listByUser(String userId, int page, int size);
    
    void deleteByIdAndUser(String id, String userId);
}