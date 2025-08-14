#!/bin/bash

# OpenLineage Insecure HTTP Transport - LocalStack Setup
# This script sets up a complete LocalStack environment for testing the transport

set -e

echo "🚀 OpenLineage Insecure HTTP Transport - LocalStack Setup"
echo "=========================================================="

# Check dependencies
check_dependency() {
    if ! command -v $1 &> /dev/null; then
        echo "❌ Error: $1 is required but not installed"
        echo "📥 Please install: $2"
        exit 1
    else
        echo "✅ $1 found"
    fi
}

echo "🔍 Checking dependencies..."
check_dependency "docker" "Docker Desktop or Docker Engine"
check_dependency "docker-compose" "Docker Compose"
check_dependency "mvn" "Apache Maven"

# Check if awslocal is available
if ! command -v awslocal &> /dev/null; then
    echo "📥 Installing awslocal..."
    if command -v pipx &> /dev/null; then
        pipx install awscli-local
        pipx ensurepath
        # Add to current session PATH
        export PATH="$HOME/.local/bin:$PATH"
    elif command -v pip3 &> /dev/null; then
        pip3 install --user awscli-local
    elif command -v pip &> /dev/null; then
        pip install --user awscli-local
    else
        echo "❌ Error: pip, pip3, or pipx is required to install awslocal"
        echo "📥 Please install with: brew install pipx"
        exit 1
    fi
fi

# Build the JAR if it doesn't exist or if source is newer
if [ ! -f "target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar" ] || [ "src" -nt "target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar" ]; then
    echo "🔨 Building OpenLineage transport JAR..."
    mvn clean package -q
    echo "✅ JAR built successfully"
else
    echo "✅ JAR is up to date"
fi

# Start LocalStack and services
echo "🐳 Starting LocalStack and services..."
docker-compose up -d

# Wait for LocalStack to be ready
echo "⏳ Waiting for LocalStack to start..."
sleep 10

# Check if LocalStack is ready
max_retries=30
retry_count=0
while [ $retry_count -lt $max_retries ]; do
    if curl -s http://localhost:4566/_localstack/health >/dev/null 2>&1; then
        echo "✅ LocalStack is ready"
        break
    fi
    retry_count=$((retry_count + 1))
    echo "⏳ Waiting for LocalStack... ($retry_count/$max_retries)"
    sleep 2
done

if [ $retry_count -eq $max_retries ]; then
    echo "❌ LocalStack failed to start after $max_retries attempts"
    exit 1
fi

# Set AWS credentials for LocalStack
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
export AWS_ENDPOINT_URL=http://localhost:4566

echo "📦 Setting up S3 buckets and uploading JAR..."

# Create S3 buckets
awslocal s3 mb s3://glue-assets-bucket 2>/dev/null || echo "📦 Bucket glue-assets-bucket already exists"
awslocal s3 mb s3://glue-scripts-bucket 2>/dev/null || echo "📦 Bucket glue-scripts-bucket already exists"
awslocal s3 mb s3://glue-data-bucket 2>/dev/null || echo "📦 Bucket glue-data-bucket already exists"

# Upload JAR to S3
echo "📤 Uploading JAR to S3..."
awslocal s3 cp target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar s3://glue-assets-bucket/jars/

# Upload Glue script to S3
echo "📤 Uploading Glue script to S3..."
awslocal s3 cp glue-scripts/test_openlineage_job.py s3://glue-scripts-bucket/

# Create IAM role for Glue
echo "🔐 Creating Glue service role..."
awslocal iam create-role \
    --role-name GlueServiceRole \
    --assume-role-policy-document '{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Principal": {
                    "Service": "glue.amazonaws.com"
                },
                "Action": "sts:AssumeRole"
            }
        ]
    }' >/dev/null 2>&1 || echo "🔐 Role GlueServiceRole already exists"

# Create Glue job
echo "⚙️  Creating Glue job with OpenLineage configuration..."
awslocal glue create-job \
    --name "openlineage-test-job" \
    --role "arn:aws:iam::000000000000:role/GlueServiceRole" \
    --command '{
        "Name": "glueetl",
        "ScriptLocation": "s3://glue-scripts-bucket/test_openlineage_job.py"
    }' \
    --default-arguments '{
        "--extra-jars": "s3://glue-assets-bucket/jars/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar",
        "--conf": "spark.openlineage.transport.type=http-insecure spark.openlineage.transport.url=http://openlineage-mock/api/v1/lineage spark.openlineage.namespace=localstack-test spark.openlineage.parentJobName=openlineage-test-job",
        "--enable-continuous-cloudwatch-log": "true",
        "--enable-spark-ui": "true",
        "--job-language": "python"
    }' \
    --max-capacity 2 \
    --timeout 60 \
    --glue-version "4.0" >/dev/null 2>&1 || echo "⚙️  Job openlineage-test-job already exists"

echo ""
echo "🎉 Setup completed successfully!"
echo ""
echo "📊 Services running:"
echo "  🔗 LocalStack:          http://localhost:4566"
echo "  🔗 Mock OpenLineage:    http://localhost:8080"
echo "  🔗 JAR Server:          http://localhost:8081"
echo ""
echo "🧪 To test the setup:"
echo "  1. Run the Glue job:    ./run_glue_job.sh"
echo "  2. Check logs:          ./check_logs.sh"
echo "  3. View lineage events: ./view_lineage_events.sh"
echo ""
echo "🛑 To stop all services:"
echo "  docker-compose down"
echo ""
echo "📋 Glue job configuration:"
echo "  Job Name: openlineage-test-job"
echo "  Transport: http-insecure"
echo "  Endpoint: http://openlineage-mock/api/v1/lineage"
echo "  Namespace: localstack-test"
