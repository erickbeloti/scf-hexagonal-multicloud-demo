#!/bin/bash

# Business rules validation test script - supports both local and deployed testing

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/test-common.sh"

USER_ID="business-test-user-$(date +%s)"
TASK_IDS=()

test_high_priority_limit() {
    print_test "Testing High Priority Task Limit (Max 5 per day)"

    local high_priority_user="high-priority-user-$(date +%s)"
    local success_count=0
    local failure_count=0

    for i in {1..6}; do
        local expected_status=200
        if [ $i -gt 5 ]; then
            expected_status=422
        fi

        local payload='{
            "userId": "'$high_priority_user'",
            "description": "High priority task '$i'",
            "priority": "HIGH"
        }'

        if call_function "createTask" "$payload" $expected_status 2>/dev/null; then
            if [ $expected_status -eq 200 ]; then
                success_count=$((success_count + 1))
                print_success "Created high priority task $i"
            else
                failure_count=$((failure_count + 1))
                print_warning "Failed to create high priority task $i (expected business rule violation)"
            fi
        else
            if [ $expected_status -eq 200 ]; then
                failure_count=$((failure_count + 1))
                print_error "Unexpectedly failed to create high priority task $i"
            else
                success_count=$((success_count + 1))
                print_warning "Unexpectedly created high priority task $i"
            fi
        fi
    done

    if [ $success_count -eq 5 ] && [ $failure_count -eq 1 ]; then
        print_success "High priority limit rule working correctly (5 allowed, 6th rejected)"
    else
        print_error "High priority limit rule failed (created: $success_count, rejected: $failure_count)"
        return 1
    fi
}

test_unique_description() {
    print_test "Testing Unique Description Rule"

    local unique_user="unique-desc-user-$(date +%s)"
    local description="Unique test description $(date +%s)"

    local task_id1=$(create_task "$description" "LOW" "$unique_user")
    if [ $? -eq 0 ]; then
        print_success "Created first task with unique description"
    else
        print_error "Failed to create first task"
        return 1
    fi

    if ! create_task "$description" "LOW" "$unique_user" 2>/dev/null; then
        print_success "Correctly rejected duplicate description"
    else
        print_error "Failed to reject duplicate description"
        return 1
    fi
}

test_valid_priority() {
    print_test "Testing Valid Priority Rule"

    local priority_user="priority-user-$(date +%s)"

    for priority in "LOW" "MEDIUM" "HIGH"; do
        if create_task "Priority test $priority" "$priority" "$priority_user" >/dev/null 2>&1; then
            print_success "Valid priority '$priority' accepted"
        else
            print_error "Valid priority '$priority' rejected"
        fi
    done

    local payload='{
        "userId": "'$priority_user'",
        "description": "Invalid priority test",
        "priority": "INVALID"
    }'

    if ! call_function "createTask" "$payload" 422 2>/dev/null; then
        print_success "Invalid priority 'INVALID' correctly rejected"
    else
        print_error "Invalid priority 'INVALID' was accepted"
    fi
}

test_user_isolation() {
    print_test "Testing User Isolation Rule"

    local user1="isolation-user1-$(date +%s)"
    local user2="isolation-user2-$(date +%s)"

    local task_id=$(create_task "User1 private task" "LOW" "$user1")
    if [ $? -ne 0 ]; then
        print_error "Failed to create task for user isolation test"
        return 1
    fi

    local payload='{
        "id": "'$task_id'",
        "userId": "'$user2'"
    }'

    if call_function "getTaskById" "$payload" 422 2>/dev/null; then
        print_success "User isolation working - user2 cannot access user1's task"
    else
        print_error "User isolation failed - user2 accessed user1's task"
        return 1
    fi
}

test_completed_task_update() {
    print_test "Testing Completed Task Update Rule"

    local completed_user="completed-user-$(date +%s)"

    local task_id=$(create_task "Task to complete" "LOW" "$completed_user")
    if [ $? -ne 0 ]; then
        print_error "Failed to create task for completion test"
        return 1
    fi

    local complete_payload='{
        "id": "'$task_id'",
        "userId": "'$completed_user'",
        "description": "Task to complete",
        "priority": "LOW",
        "status": "COMPLETED"
    }'

    if call_function "updateTask" "$complete_payload" 200 2>/dev/null; then
        print_success "Task completed successfully"
    else
        print_error "Failed to complete task"
        return 1
    fi

    local update_payload='{
        "id": "'$task_id'",
        "userId": "'$completed_user'",
        "description": "Trying to update completed task",
        "priority": "MEDIUM",
        "status": "COMPLETED"
    }'

    if call_function "updateTask" "$update_payload" 422 2>/dev/null; then
        print_success "Completed task update correctly prevented"
    else
        print_error "Completed task was updated (should be prevented)"
        return 1
    fi
}

test_delete_isolation() {
    print_test "Testing Delete Isolation Rule"

    local user1="delete-user1-$(date +%s)"
    local user2="delete-user2-$(date +%s)"

    local task_id=$(create_task "User1 task to delete" "LOW" "$user1")
    if [ $? -ne 0 ]; then
        print_error "Failed to create task for delete isolation test"
        return 1
    fi

    local payload='{
        "id": "'$task_id'",
        "userId": "'$user2'"
    }'

    if call_function "deleteTask" "$payload" 422 2>/dev/null; then
        print_success "Delete isolation working - user2 cannot delete user1's task"
    else
        print_error "Delete isolation failed - user2 deleted user1's task"
        return 1
    fi
}

main() {
    local test_mode=${1:-"auto"}

    if [ "$test_mode" = "--help" ] || [ "$test_mode" = "-h" ]; then
        show_usage
        exit 0
    fi

    print_test "Business Rules Validation Test Suite"
    echo "Testing all domain business rules implementation"

    detect_test_environment "$test_mode"
    validate_test_environment

    local failed_tests=0

    test_valid_priority || failed_tests=$((failed_tests + 1))
    test_unique_description || failed_tests=$((failed_tests + 1))
    test_high_priority_limit || failed_tests=$((failed_tests + 1))
    test_user_isolation || failed_tests=$((failed_tests + 1))
    test_completed_task_update || failed_tests=$((failed_tests + 1))
    test_delete_isolation || failed_tests=$((failed_tests + 1))

    print_test_summary "Business Rules" $failed_tests
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
