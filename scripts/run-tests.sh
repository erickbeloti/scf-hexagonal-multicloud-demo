#!/bin/bash

# Integration test runner - executes integration test suites
# Requires target environment to be already running (like GCP approach)

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/test-common.sh"

run_integration_tests() {
    print_test "Running Integration Tests"
    local test_mode=$1
    local failed_tests=0

    print_info "Running basic function tests..."
    if ! "$SCRIPT_DIR/test-functions.sh" "$test_mode"; then
        failed_tests=$((failed_tests + 1))
    fi

    print_info "Running business rules tests..."
    if ! "$SCRIPT_DIR/test-business-rules.sh" "$test_mode"; then
        failed_tests=$((failed_tests + 1))
    fi

    if [ $failed_tests -eq 0 ]; then
        print_success "All integration tests passed"
        return 0
    else
        print_error "$failed_tests integration test suite(s) failed"
        return 1
    fi
}

show_help() {
    echo "Integration Test Runner - Execute integration test suites"
    echo ""
    echo "Usage: $0 [TEST_MODE]"
    echo ""
    echo "Test modes:"
    echo "  local  - Test against local Spring Boot server"
    echo "           Prerequisites: mvn spring-boot:run -Dspring.profiles.active=local"
    echo "  gcp    - Test against deployed GCP Cloud Function"
    echo "           Prerequisites: cd terraform/gcp && terraform apply"
    echo "  auto   - Auto-detect available environment (default)"
    echo ""
    echo "Note: For unit and architecture tests, use Maven directly:"
    echo "  mvn test              # Run all unit and architecture tests"
    echo ""
    echo "Examples:"
    echo "  $0                    # Auto-detect environment and run integration tests"
    echo "  $0 local              # Run integration tests locally (server must be running)"
    echo "  $0 gcp                # Run integration tests against GCP deployment"
    echo ""
    echo "Full development workflow:"
    echo "  mvn test              # Unit and architecture tests (no server needed)"
    echo "  $0 local              # Integration tests (server required)"
}

main() {
    local test_mode=${1:-"auto"}

    if [ "$test_mode" = "--help" ] || [ "$test_mode" = "-h" ]; then
        show_help
        exit 0
    fi

    print_test "Integration Test Runner"
    print_info "Test mode: $test_mode"

    detect_test_environment "$test_mode"
    validate_test_environment

    if run_integration_tests "$test_mode"; then
        print_success "🎉 All integration tests passed!"
        exit 0
    else
        print_error "❌ Integration tests failed"
        exit 1
    fi
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
