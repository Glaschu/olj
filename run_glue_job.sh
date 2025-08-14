#!/bin/bash

# Run the Glue job in LocalStack

set -e

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
export AWS_ENDPOINT_URL=http://localhost:4566

echo "🚀 Starting Glue job: openlineage-test-job"
echo "========================================="

# Check if LocalStack is running
if ! curl -s http://localhost:4566/_localstack/health >/dev/null 2>&1; then
    echo "❌ LocalStack is not running. Please run ./setup_localstack.sh first"
    exit 1
fi

# Start the job
echo "▶️  Starting job execution..."
JOB_RUN_ID=$(awslocal glue start-job-run \
    --job-name "openlineage-test-job" \
    --query 'JobRunId' \
    --output text)

echo "✅ Job started with run ID: $JOB_RUN_ID"
echo ""
echo "📊 Monitoring job progress..."

# Monitor job status
while true; do
    JOB_STATUS=$(awslocal glue get-job-run \
        --job-name "openlineage-test-job" \
        --run-id "$JOB_RUN_ID" \
        --query 'JobRun.JobRunState' \
        --output text)
    
    echo "📋 Job status: $JOB_STATUS"
    
    case $JOB_STATUS in
        "SUCCEEDED")
            echo "🎉 Job completed successfully!"
            break
            ;;
        "FAILED"|"STOPPED"|"ERROR")
            echo "❌ Job failed with status: $JOB_STATUS"
            echo "📋 Getting job details..."
            awslocal glue get-job-run \
                --job-name "openlineage-test-job" \
                --run-id "$JOB_RUN_ID" \
                --query 'JobRun.ErrorMessage' \
                --output text
            break
            ;;
        "RUNNING"|"STARTING")
            echo "⏳ Job is still running... (will check again in 10 seconds)"
            sleep 10
            ;;
        *)
            echo "🔄 Job status: $JOB_STATUS (checking again in 5 seconds)"
            sleep 5
            ;;
    esac
done

echo ""
echo "📊 Job execution completed"
echo "🔍 Run './check_logs.sh' to view detailed logs"
echo "📨 Run './view_lineage_events.sh' to see lineage events"
