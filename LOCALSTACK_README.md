# OpenLineage Insecure HTTP Transport - LocalStack Testing

This setup provides a complete local testing environment for the OpenLineage insecure HTTP transport using LocalStack and AWS Glue.

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────────┐
│   LocalStack    │    │  Mock OpenLineage │    │    JAR Server       │
│   (AWS Glue)    │────│     Server        │    │  (Static Files)     │
│  localhost:4566 │    │  localhost:8080   │    │  localhost:8081     │
└─────────────────┘    └──────────────────┘    └─────────────────────┘
        │                        │                        │
        │                        │                        │
        └────────────────────────┼────────────────────────┘
                                 │
                    ┌─────────────────────┐
                    │  Glue Job Execution │
                    │  - Loads JAR from   │
                    │    localhost:8081   │
                    │  - Sends lineage to │
                    │    localhost:8080   │
                    └─────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 8+ and Maven
- Python 3.7+ (with pip)

### 1. Setup and Start Services

```bash
# Clone and build the project
git clone <your-repo>
cd olj

# Start all services (this will build the JAR, start LocalStack, create resources)
./setup_localstack.sh
```

This script will:
- ✅ Build the OpenLineage transport JAR
- ✅ Start LocalStack with Glue service
- ✅ Start mock OpenLineage server
- ✅ Start JAR file server
- ✅ Create S3 buckets and upload assets
- ✅ Create IAM roles for Glue
- ✅ Create and configure the Glue job

### 2. Run the Test Job

```bash
# Execute the Glue job that uses the insecure HTTP transport
./run_glue_job.sh
```

### 3. Check Results

```bash
# View job logs and status
./check_logs.sh

# View lineage events received by the mock server
./view_lineage_events.sh
```

### 4. Cleanup

```bash
# Stop all services
docker-compose down
```

## 📋 Services Overview

### LocalStack (AWS Glue)
- **Port**: 4566
- **Purpose**: Simulates AWS Glue service locally
- **Endpoint**: http://localhost:4566
- **Services**: S3, Glue, IAM, CloudWatch Logs

### Mock OpenLineage Server
- **Port**: 8080
- **Purpose**: Receives and logs lineage events
- **Endpoints**:
  - `GET /health` - Health check
  - `POST /api/v1/lineage` - Lineage events endpoint
- **Features**: CORS enabled, request logging

### JAR Server
- **Port**: 8081
- **Purpose**: Serves the built JAR file to Glue jobs
- **Contents**: Static file server with build artifacts

## ⚙️ Configuration

### Glue Job Configuration

The Glue job is configured with these OpenLineage settings:

```bash
--extra-jars: s3://glue-assets-bucket/jars/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar
--conf: spark.openlineage.transport.type=http-insecure
--conf: spark.openlineage.transport.url=http://openlineage-mock/api/v1/lineage
--conf: spark.openlineage.namespace=localstack-test
--conf: spark.openlineage.parentJobName=openlineage-test-job
```

### Test Job Details

The test job (`glue-scripts/test_openlineage_job.py`):
- Creates sample employee data
- Performs filtering and aggregation operations
- Writes results to S3
- Tests direct HTTP connection to lineage endpoint
- All operations generate lineage events via the insecure transport

## 🧪 Testing Scenarios

### 1. Basic Transport Test
```bash
./setup_localstack.sh
./run_glue_job.sh
./view_lineage_events.sh
```

### 2. Manual Event Test
```bash
# Send a test event directly to the mock server
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "START",
    "eventTime": "2025-08-14T19:00:00.000Z",
    "job": {"namespace": "test", "name": "manual-test"},
    "run": {"runId": "manual-test-123"}
  }' \
  http://localhost:8080/api/v1/lineage
```

### 3. SSL Bypass Verification
The mock server doesn't use HTTPS, but you can test SSL bypass by:
1. Setting up a self-signed HTTPS endpoint
2. Configuring the transport to use that endpoint
3. Verifying that SSL errors don't occur

## 📊 Monitoring and Logs

### Real-time Monitoring
```bash
# Watch LocalStack logs
docker logs -f localstack-glue

# Watch OpenLineage server logs
docker logs -f openlineage-mock

# Watch all services
docker-compose logs -f
```

### Log Locations
- **Glue Job Logs**: Available via `awslocal logs` commands
- **OpenLineage Events**: `/var/log/nginx/lineage_events.log` in mock container
- **HTTP Access**: `/var/log/nginx/lineage_access.log` in mock container

## 🔧 Customization

### Modify Glue Job
Edit `glue-scripts/test_openlineage_job.py` to:
- Add more complex transformations
- Test different data sources
- Simulate various lineage scenarios

### Change OpenLineage Configuration
Modify the job arguments in `setup_localstack.sh`:
```bash
"--conf": "spark.openlineage.transport.type=http-insecure spark.openlineage.transport.url=http://your-endpoint/api/v1/lineage"
```

### Mock Server Configuration
Edit `mock-lineage-config/default.conf` to:
- Add authentication
- Modify response behavior
- Add custom endpoints

## 🐛 Troubleshooting

### Common Issues

1. **LocalStack not starting**
   ```bash
   # Check Docker is running
   docker ps
   
   # Check logs
   docker-compose logs localstack
   ```

2. **JAR not found**
   ```bash
   # Rebuild and reupload
   mvn clean package
   awslocal s3 cp target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar s3://glue-assets-bucket/jars/
   ```

3. **No lineage events received**
   ```bash
   # Check job status
   awslocal glue get-job-runs --job-name openlineage-test-job
   
   # Check mock server logs
   docker logs openlineage-mock
   ```

4. **Network connectivity issues**
   ```bash
   # Test connectivity from within LocalStack
   docker exec localstack-glue curl http://openlineage-mock/health
   ```

### Debug Commands

```bash
# Check service health
curl http://localhost:4566/_localstack/health
curl http://localhost:8080/health

# List S3 contents
awslocal s3 ls s3://glue-assets-bucket/jars/

# Get job details
awslocal glue get-job --job-name openlineage-test-job

# Check IAM roles
awslocal iam list-roles
```

## 📁 File Structure

```
.
├── docker-compose.yml              # Docker services configuration
├── setup_localstack.sh             # Main setup script
├── run_glue_job.sh                 # Job execution script
├── check_logs.sh                   # Log viewing script
├── view_lineage_events.sh          # Lineage events viewer
├── glue-scripts/
│   └── test_openlineage_job.py     # Test Glue job
├── mock-lineage-config/
│   └── default.conf                # Nginx configuration for mock server
└── localstack-init/
    └── 01-setup.sh                 # LocalStack initialization script
```

## 🎯 Expected Results

When everything is working correctly, you should see:

1. **Successful job execution** with status "SUCCEEDED"
2. **Lineage events in mock server logs** showing HTTP POST requests
3. **No SSL errors** in job logs (proving the insecure transport works)
4. **Output data in S3** at `s3://glue-data-bucket/output/employee_summary/`

The insecure HTTP transport successfully bypasses SSL validation and delivers lineage events to the configured endpoint!
