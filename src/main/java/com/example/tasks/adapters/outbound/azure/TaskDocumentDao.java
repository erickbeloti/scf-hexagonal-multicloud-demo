package com.example.tasks.adapters.outbound.azure;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data Cosmos repository for TaskDocument.
 * Provides query methods for Cosmos DB operations.
 */
@Repository
@Profile("azure")
public interface TaskDocumentDao extends CosmosRepository<TaskDocument, String> {

    /**
     * Find all tasks for a specific user.
     */
    List<TaskDocument> findByUserId(String userId);

    /**
     * Find tasks by user and status.
     */
    @Query("SELECT * FROM c WHERE c.userId = @userId AND c.status = @status")
    List<TaskDocument> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") String status);

    /**
     * Find tasks by user, priority and date range.
     * Used for counting high priority tasks per day.
     */
    @Query("SELECT * FROM c WHERE c.userId = @userId AND c.priority = @priority AND c.createdAt >= @startDate AND c.createdAt < @endDate")
    List<TaskDocument> findByUserIdAndPriorityAndCreatedAtBetween(
        @Param("userId") String userId,
        @Param("priority") String priority,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate
    );

    /**
     * Find tasks by user, description and date range.
     * Used for checking description uniqueness per day.
     */
    @Query("SELECT * FROM c WHERE c.userId = @userId AND c.description = @description AND c.createdAt >= @startDate AND c.createdAt < @endDate")
    List<TaskDocument> findByUserIdAndDescriptionAndCreatedAtBetween(
        @Param("userId") String userId,
        @Param("description") String description,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate
    );
}

