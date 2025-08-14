#!/bin/bash

# OpenLineage Insecure HTTP Transport - Status Summary

echo "🚀 OpenLineage Insecure HTTP Transport - Status Summary"
echo "======================================================="
echo ""

# Check if JAR exists
if [ -f "target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar" ]; then
    JAR_SIZE=$(ls -lh target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar | awk '{print $5}')
    echo "✅ JAR built: openlineage-transport-http-insecure-1.0-SNAPSHOT.jar ($JAR_SIZE)"
else
    echo "❌ JAR not built - run 'mvn clean package'"
fi

# Check if Docker is running
if command -v docker &> /dev/null && docker info &> /dev/null; then
    echo "✅ Docker is running"
    
    # Check if our containers are running
    if docker ps | grep -q "localstack-glue"; then
        echo "✅ LocalStack container is running"
    else
        echo "❌ LocalStack container not running"
    fi
    
    if docker ps | grep -q "openlineage-mock"; then
        echo "✅ Mock OpenLineage server is running"
    else
        echo "❌ Mock OpenLineage server not running"
    fi
    
    if docker ps | grep -q "jar-server"; then
        echo "✅ JAR server is running"
    else
        echo "❌ JAR server not running"
    fi
else
    echo "❌ Docker is not running or not available"
fi

echo ""
echo "🧪 Test Results (last run):"
echo "=========================="

# Check if tests have been run
if [ -d "target/surefire-reports" ]; then
    TEST_FILES=$(find target/surefire-reports -name "TEST-*.xml")
    if [ -n "$TEST_FILES" ]; then
        TOTAL_TESTS=$(echo "$TEST_FILES" | wc -l)
        FAILED_TESTS=$(echo "$TEST_FILES" | xargs grep -l '<failure\|<error' | wc -l)
        
        if [ $FAILED_TESTS -eq 0 ]; then
            echo "✅ All tests passing ($TOTAL_TESTS test classes)"
        else
            echo "❌ Some tests failed ($FAILED_TESTS failed out of $TOTAL_TESTS)"
        fi
    else
        echo "❓ No test reports found"
    fi
else
    echo "❓ Tests not run - run 'mvn test'"
fi

echo ""
echo "🌐 Service Endpoints:"
echo "===================="

# Check service availability
check_endpoint() {
    local url=$1
    local name=$2
    if curl -s "$url" >/dev/null 2>&1; then
        echo "✅ $name: $url"
    else
        echo "❌ $name: $url (not responding)"
    fi
}

check_endpoint "http://localhost:4566/_localstack/health" "LocalStack"
check_endpoint "http://localhost:8080/health" "Mock OpenLineage"
check_endpoint "http://localhost:8081" "JAR Server"

echo ""
echo "📊 Recent Activity:"
echo "=================="

# Check if there's been recent activity
if command -v awslocal &> /dev/null && curl -s http://localhost:4566/_localstack/health >/dev/null 2>&1; then
    export AWS_ACCESS_KEY_ID=test
    export AWS_SECRET_ACCESS_KEY=test
    export AWS_DEFAULT_REGION=us-east-1
    export AWS_ENDPOINT_URL=http://localhost:4566
    
    # Check for recent job runs
    RECENT_JOBS=$(awslocal glue get-job-runs --job-name "openlineage-test-job" --query 'length(JobRuns)' --output text 2>/dev/null || echo "0")
    if [ "$RECENT_JOBS" != "0" ] && [ "$RECENT_JOBS" != "None" ]; then
        echo "✅ Found $RECENT_JOBS Glue job runs"
        
        LATEST_STATUS=$(awslocal glue get-job-runs --job-name "openlineage-test-job" --query 'JobRuns[0].JobRunState' --output text 2>/dev/null || echo "Unknown")
        echo "📋 Latest job status: $LATEST_STATUS"
    else
        echo "❓ No Glue job runs found"
    fi
    
    # Check S3 buckets
    BUCKETS=$(awslocal s3 ls | wc -l 2>/dev/null || echo "0")
    echo "📦 S3 buckets created: $BUCKETS"
    
else
    echo "❓ LocalStack not available for activity check"
fi

echo ""
echo "🎯 Quick Actions:"
echo "================"
echo "📦 Setup environment:     ./setup_localstack.sh"
echo "🚀 Run Glue job:          ./run_glue_job.sh"
echo "📋 Check logs:            ./check_logs.sh"
echo "📨 View lineage events:   ./view_lineage_events.sh"
echo "🧪 Run unit tests:        mvn test"
echo "🔨 Build JAR:             mvn clean package"
echo "🛑 Stop services:         docker-compose down"

echo ""
echo "📚 Documentation:"
echo "================="
echo "📖 LocalStack Guide:      LOCALSTACK_README.md"
echo "🚀 Deployment Guide:      DEPLOYMENT_GUIDE.md"
echo "📋 Project README:        README.md"

echo ""
if [ -f "target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar" ] && curl -s http://localhost:4566/_localstack/health >/dev/null 2>&1; then
    echo "🎉 Environment is ready for testing!"
else
    echo "⚙️  Run './setup_localstack.sh' to initialize the environment"
fi
