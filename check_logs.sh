#!/bin/bash

# Check logs from the Glue job and services

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
export AWS_ENDPOINT_URL=http://localhost:4566

echo "📋 OpenLineage Transport Logs"
echo "============================="

# Check if services are running
if ! curl -s http://localhost:4566/_localstack/health >/dev/null 2>&1; then
    echo "❌ LocalStack is not running"
    exit 1
fi

if ! curl -s http://localhost:8080/health >/dev/null 2>&1; then
    echo "❌ Mock OpenLineage service is not running"
    exit 1
fi

echo "✅ All services are running"
echo ""

# Get the latest job run
echo "🔍 Finding latest Glue job run..."
LATEST_RUN=$(awslocal glue get-job-runs \
    --job-name "openlineage-test-job" \
    --query 'JobRuns[0].Id' \
    --output text 2>/dev/null || echo "none")

if [ "$LATEST_RUN" = "none" ] || [ "$LATEST_RUN" = "None" ]; then
    echo "📋 No job runs found. Run './run_glue_job.sh' first"
    exit 1
fi

echo "📋 Latest job run ID: $LATEST_RUN"
echo ""

# Get job run details
echo "📊 Job Run Details:"
echo "=================="
awslocal glue get-job-run \
    --job-name "openlineage-test-job" \
    --run-id "$LATEST_RUN" \
    --query '{
        Status: JobRun.JobRunState,
        StartedOn: JobRun.StartedOn,
        CompletedOn: JobRun.CompletedOn,
        ExecutionTime: JobRun.ExecutionTime,
        ErrorMessage: JobRun.ErrorMessage
    }' \
    --output table

echo ""
echo "📨 Mock OpenLineage Server Logs:"
echo "==============================="

# Get logs from the mock OpenLineage server
echo "🔍 Checking for received lineage events..."
docker logs openlineage-mock --tail 50 2>/dev/null || echo "No logs available from openlineage-mock container"

echo ""
echo "🐳 LocalStack Container Logs:"
echo "============================"

# Get LocalStack logs (last 30 lines)
echo "🔍 Recent LocalStack activity..."
docker logs localstack-glue --tail 30 2>/dev/null || echo "No logs available from localstack-glue container"

echo ""
echo "📊 Service Status Summary:"
echo "========================="
echo "LocalStack Health: $(curl -s http://localhost:4566/_localstack/health | jq -r '.services.glue // "Unknown"' 2>/dev/null || echo "Unknown")"
echo "Mock OpenLineage: $(curl -s http://localhost:8080/health | jq -r '.status // "Unknown"' 2>/dev/null || echo "Unknown")"
echo "JAR Server: $(curl -s http://localhost:8081 >/dev/null 2>&1 && echo "Running" || echo "Not responding")"
