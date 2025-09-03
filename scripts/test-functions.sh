#!/bin/bash

# Basic function tests - supports both local and deployed testing
# Tests CRUD operations: create, read, update, delete, list tasks

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/test-common.sh"

USER_ID="test-user-$(date +%s)"
TASK_ID=""

call_function_with_logging() {
    local function_name=$1
    local payload=$2
    local expected_status=${3:-200}

    echo "Calling $function_name with payload: $payload"

    if call_function "$function_name" "$payload" "$expected_status"; then
        print_success "HTTP $http_code - $function_name succeeded"
        echo "Response: $response_body"
        return 0
    else
        print_error "HTTP $http_code - $function_name failed (expected $expected_status)"
        echo "Response: $response_body"
        return 1
    fi
}

test_create_task() {
    print_test "Testing Create Task"

    local payload='{
        "userId": "'$USER_ID'",
        "description": "Test task created by script",
        "priority": "HIGH"
    }'

    if call_function_with_logging "createTask" "$payload"; then
        TASK_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$TASK_ID" ]; then
            print_success "Task created with ID: $TASK_ID"
        else
            print_warning "Task created but ID not found in response"
        fi
    else
        print_error "Failed to create task"
        return 1
    fi
}

test_list_tasks() {
    print_test "Testing List Tasks"

    local payload='{
        "userId": "'$USER_ID'",
        "page": 0,
        "size": 10
    }'

    call_function_with_logging "listTasksByUser" "$payload"
}

test_get_task() {
    print_test "Testing Get Task by ID"

    if [ -z "$TASK_ID" ]; then
        print_warning "Skipping get task test - no task ID available"
        return 0
    fi

    local payload='{
        "id": "'$TASK_ID'",
        "userId": "'$USER_ID'"
    }'

    call_function_with_logging "getTaskById" "$payload"
}

test_update_task() {
    print_test "Testing Update Task"

    if [ -z "$TASK_ID" ]; then
        print_warning "Skipping update task test - no task ID available"
        return 0
    fi

    local payload='{
        "id": "'$TASK_ID'",
        "userId": "'$USER_ID'",
        "description": "Updated test task",
        "priority": "MEDIUM",
        "status": "COMPLETED"
    }'

    call_function_with_logging "updateTask" "$payload"
}

test_delete_task() {
    print_test "Testing Delete Task"

    if [ -z "$TASK_ID" ]; then
        print_warning "Skipping delete task test - no task ID available"
        return 0
    fi

    local payload='{
        "id": "'$TASK_ID'",
        "userId": "'$USER_ID'"
    }'

    call_function_with_logging "deleteTask" "$payload" 204
}

main() {
    local test_mode=${1:-"auto"}

    if [ "$test_mode" = "--help" ] || [ "$test_mode" = "-h" ]; then
        show_usage
        exit 0
    fi

    print_test "Basic Function Tests - CRUD Operations"
    echo "Starting tests with user ID: $USER_ID"

    detect_test_environment "$test_mode"
    validate_test_environment

    local failed_tests=0

    test_create_task || failed_tests=$((failed_tests + 1))
    test_list_tasks || failed_tests=$((failed_tests + 1))
    test_get_task || failed_tests=$((failed_tests + 1))
    test_update_task || failed_tests=$((failed_tests + 1))
    test_delete_task || failed_tests=$((failed_tests + 1))

    print_test_summary "Basic Functions" $failed_tests
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
