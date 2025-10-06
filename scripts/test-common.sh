#!/bin/bash

# Common test functions and utilities for both local and deployed testing

set -e

# Configuration
DEFAULT_LOCAL_URL="http://localhost:8080"
DEFAULT_GCP_URL=""
DEFAULT_AWS_URL=""
FUNCTION_URL=""
TEST_MODE=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${2}${1}${NC}"
}

print_test() {
    echo -e "\n${BLUE}=== $1 ===${NC}"
}

print_success() {
    print_status "✓ $1" $GREEN
}

print_error() {
    print_status "✗ $1" $RED
}

print_warning() {
    print_status "⚠ $1" $YELLOW
}

print_info() {
    print_status "ℹ $1" $BLUE
}

detect_test_environment() {
    local mode=${1:-"auto"}

    case $mode in
        "local")
            TEST_MODE="local"
            FUNCTION_URL="$DEFAULT_LOCAL_URL"
            print_info "Testing mode: LOCAL ($FUNCTION_URL)"
            ;;
        "gcp")
            TEST_MODE="gcp"
            get_gcp_function_url
            print_info "Testing mode: GCP ($FUNCTION_URL)"
            ;;
        "aws")
            TEST_MODE="aws"
            get_aws_function_url
            print_info "Testing mode: AWS ($FUNCTION_URL)"
            ;;
        "auto")
            # Try to detect automatically
            if check_local_server; then
                TEST_MODE="local"
                FUNCTION_URL="$DEFAULT_LOCAL_URL"
                print_info "Auto-detected mode: LOCAL ($FUNCTION_URL)"
            elif get_gcp_function_url; then
                TEST_MODE="gcp"
                print_info "Auto-detected mode: GCP ($FUNCTION_URL)"
            elif get_aws_function_url; then
                TEST_MODE="aws"
                print_info "Auto-detected mode: AWS ($FUNCTION_URL)"
            else
                print_error "Could not detect test environment."
                print_info "Make sure either:"
                print_info "  - Local server is running: mvn spring-boot:run -Dspring.profiles.active=local"
                print_info "  - GCP function is deployed: cd terraform/gcp && terraform apply"
                print_info "  - AWS function is deployed: cd terraform/aws && terraform apply"
                exit 1
            fi
            ;;
        *)
            print_error "Invalid test mode: $mode. Use 'local', 'gcp', 'aws', or 'auto'"
            exit 1
            ;;
    esac
}

check_local_server() {
    curl -s -f "$DEFAULT_LOCAL_URL/actuator/health" > /dev/null 2>&1
    return $?
}

get_gcp_function_url() {
    if [ -d "terraform/gcp" ]; then
        cd terraform/gcp
        FUNCTION_URL=$(terraform output -raw function_url 2>/dev/null || echo "")
        cd - > /dev/null
    fi

    if [ -z "$FUNCTION_URL" ]; then
        print_warning "Could not get GCP function URL from Terraform."
        return 1
    fi

    return 0
}

get_aws_function_url() {
    if [ -d "terraform/aws" ]; then
        cd terraform/aws
        FUNCTION_URL=$(terraform output -raw function_url 2>/dev/null || echo "")
        cd - > /dev/null
    fi

    if [ -z "$FUNCTION_URL" ]; then
        print_warning "Could not get AWS function URL from Terraform."
        return 1
    fi

    return 0
}

call_function() {
    local function_name=$1
    local payload=$2
    local expected_status=${3:-200}

    if [ "$TEST_MODE" = "local" ]; then
        call_function_local "$function_name" "$payload" "$expected_status"
    elif [ "$TEST_MODE" = "gcp" ]; then
        call_function_gcp "$function_name" "$payload" "$expected_status"
    else
        call_function_aws "$function_name" "$payload" "$expected_status"
    fi
}

call_function_local() {
    local function_name=$1
    local payload=$2
    local expected_status=${3:-200}

    # Use a single curl call with verbose output to get both headers and body
    response=$(curl -s -v -w "\n%{http_code}" \
        -X POST "$FUNCTION_URL/" \
        -H "Content-Type: application/json" \
        -H "function.name: $function_name" \
        -d "$payload" 2>&1)

    parse_response_and_check_status_local "$function_name" "$expected_status"
}

call_function_gcp() {
    local function_name=$1
    local payload=$2
    local expected_status=${3:-200}

    response=$(curl -s -w "\n%{http_code}" \
        -X POST "$FUNCTION_URL" \
        -H "Content-Type: application/json" \
        -H "function.name: $function_name" \
        -d "$payload")

    parse_response_and_check_status_gcp "$function_name" "$expected_status"
}

call_function_aws() {
    local function_name=$1
    local payload=$2
    local expected_status=${3:-200}

    response=$(curl -s -w "\n%{http_code}" \
        -X POST "$FUNCTION_URL" \
        -H "Content-Type: application/json" \
        -H "function.name: $function_name" \
        -d "$payload")

    parse_response_and_check_status_aws "$function_name" "$expected_status"
}

parse_response_and_check_status_local() {
    local function_name=$1
    local expected_status=$2

    # Extract HTTP status code from the last line
    http_code=$(echo "$response" | tail -n1)

    # Extract status from custom header from verbose output
    local header_status=$(echo "$response" | grep -i "< statuscode:" | sed 's/.*: *//' | tr -d '\r\n')

    # Extract response body - find the JSON response between curl verbose output
    # Look for lines that start with { and contain JSON
    response_body=$(echo "$response" | grep -E '^{.*}$' | head -n1)

    # For local testing, use the header status (which contains the real status code)
    # The HTTP status will always be 200 for Spring Cloud Function web mode
    local actual_status=$header_status

    if [ "$actual_status" = "$expected_status" ]; then
        return 0
    else
        # Debug output for troubleshooting
        echo "Debug: Expected=$expected_status, Header=$header_status, HTTP=$http_code" >&2
        echo "Debug: Response body: $response_body" >&2
        return 1
    fi
}

parse_response_and_check_status_gcp() {
    local function_name=$1
    local expected_status=$2

    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)

    # For GCP, check HTTP status code (which comes from statusCode header)
    if [ "$http_code" -eq "$expected_status" ]; then
        return 0
    else
        return 1
    fi
}

parse_response_and_check_status_aws() {
    local function_name=$1
    local expected_status=$2

    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)

    # For AWS, check HTTP status code
    if [ "$http_code" -eq "$expected_status" ]; then
        return 0
    else
        return 1
    fi
}

parse_response_and_check_status() {
    local function_name=$1
    local expected_status=$2

    if [ "$TEST_MODE" = "local" ]; then
        parse_response_and_check_status_local "$function_name" "$expected_status"
    elif [ "$TEST_MODE" = "gcp" ]; then
        parse_response_and_check_status_gcp "$function_name" "$expected_status"
    else
        parse_response_and_check_status_aws "$function_name" "$expected_status"
    fi
}

validate_test_environment() {
    case $TEST_MODE in
        "local")
            if ! check_local_server; then
                print_error "Local server is not running"
                print_info "Start the server first: mvn spring-boot:run -Dspring.profiles.active=local"
                exit 1
            fi
            print_success "Local server is accessible"
            ;;
        "gcp")
            # Test GCP function accessibility with a simple request
            if ! curl -s -f "$FUNCTION_URL" > /dev/null 2>&1; then
                print_warning "GCP function may not be accessible or deployed"
                print_info "Make sure: cd terraform/gcp && terraform apply"
            else
                print_success "GCP function is accessible"
            fi
            ;;
        "aws")
            # Test AWS function accessibility with a simple request
            if ! curl -s -f "$FUNCTION_URL" > /dev/null 2>&1; then
                print_warning "AWS function may not be accessible or deployed"
                print_info "Make sure: cd terraform/aws && terraform apply"
            else
                print_success "AWS function is accessible"
            fi
            ;;
    esac
}

create_task() {
    local description=$1
    local priority=${2:-"LOW"}
    local user_id=${3:-$USER_ID}

    local payload='{
        "userId": "'$user_id'",
        "description": "'$description'",
        "priority": "'$priority'"
    }'

    if call_function "createTask" "$payload"; then
        local task_id=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        echo "$task_id"
        return 0
    else
        return 1
    fi
}

print_test_summary() {
    local test_name=$1
    local failed_tests=$2

    print_test "$test_name Test Suite Complete"

    if [ $failed_tests -eq 0 ]; then
        print_success "All tests passed!"
    else
        print_error "$failed_tests test(s) failed"
        exit 1
    fi
}

show_usage() {
    echo "Usage: $0 [local|gcp|aws|auto]"
    echo ""
    echo "Test modes:"
    echo "  local  - Test against local Spring Boot server (http://localhost:8080)"
    echo "           Prerequisites: mvn spring-boot:run -Dspring.profiles.active=local"
    echo "  gcp    - Test against deployed GCP Cloud Function"
    echo "           Prerequisites: cd terraform/gcp && terraform apply"
    echo "  aws    - Test against deployed AWS Lambda Function"
    echo "           Prerequisites: cd terraform/aws && terraform apply"
    echo "  auto   - Auto-detect available environment (default)"
    echo ""
    echo "Examples:"
    echo "  $0 local   # Test locally (server must be running)"
    echo "  $0 gcp     # Test GCP deployment"
    echo "  $0 aws     # Test AWS deployment"
    echo "  $0         # Auto-detect environment"
}
