#!/bin/bash

# LocalStack initialization script for Glue setup
echo "🚀 Setting up LocalStack for OpenLineage Glue testing..."

# Set LocalStack endpoint
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# Create S3 bucket for Glue assets
echo "📦 Creating S3 bucket for Glue assets..."
awslocal s3 mb s3://glue-assets-bucket
awslocal s3 mb s3://glue-scripts-bucket
awslocal s3 mb s3://glue-data-bucket

# Upload our JAR to S3 (we'll do this after building)
echo "📄 S3 buckets created successfully"

# Create IAM role for Glue
echo "🔐 Creating IAM role for Glue..."
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
    }'

# Attach policies to the role
awslocal iam attach-role-policy \
    --role-name GlueServiceRole \
    --policy-arn arn:aws:iam::aws:policy/service-role/AWSGlueServiceRole

awslocal iam attach-role-policy \
    --role-name GlueServiceRole \
    --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess

echo "✅ LocalStack setup completed!"
echo "🌐 LocalStack endpoint: http://localhost:4566"
echo "📊 Mock OpenLineage endpoint: http://localhost:8080"
echo "📦 JAR server: http://localhost:8081"
