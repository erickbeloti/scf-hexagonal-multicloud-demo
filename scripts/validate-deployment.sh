#!/bin/bash

# Deployment validation script

set -e

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

check_terraform_deployment() {
    print_test "Checking Terraform Deployment"

    if [ ! -d "terraform/gcp" ]; then
        print_error "terraform/gcp directory not found"
        return 1
    fi

    cd terraform/gcp

    if [ ! -d ".terraform" ]; then
        print_warning "Terraform not initialized. Run: terraform init"
        cd - > /dev/null
        return 1
    fi

    if [ ! -f "terraform.tfstate" ]; then
        print_warning "No Terraform state found. Run: terraform apply"
        cd - > /dev/null
        return 1
    fi

    print_success "Terraform deployment found"
    cd - > /dev/null
    return 0
}

validate_function_url() {
    print_test "Validating Function URL"

    cd terraform/gcp
    FUNCTION_URL=$(terraform output -raw function_url 2>/dev/null || echo "")
    cd - > /dev/null

    if [ -z "$FUNCTION_URL" ]; then
        print_error "Could not get function URL from Terraform output"
        return 1
    fi

    print_success "Function URL: $FUNCTION_URL"

    response=$(curl -s -o /dev/null -w "%{http_code}" "$FUNCTION_URL" || echo "000")

    if [ "$response" != "000" ]; then
        print_success "Function endpoint is reachable (HTTP $response)"
        return 0
    else
        print_error "Function endpoint not reachable"
        return 1
    fi
}

test_basic_function() {
    print_test "Testing Basic Function Call"

    local timestamp=$(date +%s)
    local unique_user="smoke-test-user-$timestamp"

    local payload='{
        "userId": "'$unique_user'",
        "description": "Smoke test task '$timestamp'",
        "priority": "LOW"
    }'

    response=$(curl -s -w "\n%{http_code}" \
        -X POST "$FUNCTION_URL" \
        -H "Content-Type: application/json" \
        -H "function.name: createTask" \
        -d "$payload")

    http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        print_success "Basic function call working (HTTP $http_code)"
        return 0
    else
        print_error "Basic function call failed (HTTP $http_code)"
        echo "Response: $(echo "$response" | head -n -1)"
        return 1
    fi
}

check_gcp_resources() {
    print_test "Checking GCP Resources"

    if ! command -v gcloud &> /dev/null; then
        print_warning "gcloud CLI not found. Install from: https://cloud.google.com/sdk/docs/install"
        return 1
    fi

    local project=$(gcloud config get-value project 2>/dev/null || echo "")
    if [ -z "$project" ]; then
        print_warning "No GCP project set. Run: gcloud config set project YOUR_PROJECT_ID"
        return 1
    fi

    print_success "GCP project: $project"

    local functions=$(gcloud functions list --regions=us-central1 --format="value(name)" 2>/dev/null | grep -c "task-management" || echo "0")

    if [ "$functions" -gt 0 ]; then
        print_success "Cloud Function deployed"
    else
        print_warning "Cloud Function not found in us-central1 region"
    fi

    local databases=$(gcloud firestore databases list --format="value(name)" 2>/dev/null | wc -l)

    if [ "$databases" -gt 0 ]; then
        print_success "Firestore database exists"
    else
        print_warning "Firestore database not found"
    fi

    return 0
}

main() {
    print_test "GCP Deployment Smoke Test"
    echo "Quick validation of GCP deployment status"

    local failed_checks=0

    check_terraform_deployment || failed_checks=$((failed_checks + 1))
    check_gcp_resources || failed_checks=$((failed_checks + 1))
    validate_function_url || failed_checks=$((failed_checks + 1))
    test_basic_function || failed_checks=$((failed_checks + 1))

    print_test "Smoke Test Complete"

    if [ $failed_checks -eq 0 ]; then
        print_success "All smoke tests passed! Deployment is working correctly."
        echo ""
        echo "Next steps:"
        echo "  • Run comprehensive tests: ./scripts/test-gcp.sh"
        echo "  • Validate business rules: ./scripts/test-business-rules.sh"
        echo "  • Check logs: gcloud functions logs read task-management-dev --region=us-central1"
    else
        print_error "$failed_checks validation checks failed"
        echo ""
        echo "Troubleshooting:"
        echo "  • Check Terraform deployment: cd terraform/gcp && terraform plan"
        echo "  • Verify GCP authentication: gcloud auth list"
        echo "  • Check function logs: gcloud functions logs read task-management-dev --region=us-central1"
        exit 1
    fi
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
