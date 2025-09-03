package com.example.tasks.domain;

/**
 * Centralized domain constants for business rules.
 * This class follows the DRY (Don't Repeat Yourself) principle by defining
 * all business rule constants in one place.
 *
 * These constants represent core business invariants that should never
 * be duplicated across the codebase.
 */
public final class TaskBusinessRules {

    private TaskBusinessRules() {
        // Utility class - prevent instantiation
    }

    /**
     * Maximum number of high priority tasks a user can create per day.
     * Business Rule: Prevents users from creating too many urgent tasks.
     */
    public static final int MAX_HIGH_PRIORITY_TASKS_PER_DAY = 5;

    /**
     * Maximum number of open tasks a user can have at any time.
     * Business Rule: Prevents task overload and maintains system performance.
     */
    public static final int MAX_OPEN_TASKS_PER_USER = 50;

    /**
     * Maximum length for task descriptions.
     * Business Rule: Ensures descriptions are concise and database-friendly.
     */
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    /**
     * Minimum length for task descriptions.
     * Business Rule: Ensures descriptions are meaningful.
     */
    public static final int MIN_DESCRIPTION_LENGTH = 3;
}
