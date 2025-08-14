#!/bin/bash

# View lineage events received by the mock server

echo "📨 OpenLineage Events Viewer"
echo "============================"

# Check if mock server is running
if ! curl -s http://localhost:8080/health >/dev/null 2>&1; then
    echo "❌ Mock OpenLineage server is not running"
    echo "💡 Run './setup_localstack.sh' to start services"
    exit 1
fi

echo "✅ Mock OpenLineage server is running"
echo ""

# Check server logs for lineage events
echo "🔍 Recent lineage events received:"
echo "=================================="

# Get nginx access logs that contain lineage events
CONTAINER_LOGS=$(docker exec openlineage-mock cat /var/log/nginx/lineage_events.log 2>/dev/null || echo "")

if [ -z "$CONTAINER_LOGS" ]; then
    echo "📭 No lineage events received yet"
    echo ""
    echo "💡 Possible reasons:"
    echo "   - Glue job hasn't run yet (run './run_glue_job.sh')"
    echo "   - OpenLineage transport not configured correctly"
    echo "   - Network connectivity issues"
else
    echo "$CONTAINER_LOGS"
fi

echo ""
echo "🌐 All HTTP requests to mock server:"
echo "==================================="

# Get all access logs
ALL_LOGS=$(docker exec openlineage-mock cat /var/log/nginx/lineage_access.log 2>/dev/null || echo "")

if [ -z "$ALL_LOGS" ]; then
    echo "📭 No HTTP requests received"
else
    echo "$ALL_LOGS" | tail -20  # Show last 20 requests
fi

echo ""
echo "🧪 Test direct connection:"
echo "========================="

# Test the endpoint directly
echo "📡 Testing POST to /api/v1/lineage..."
RESPONSE=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -d '{
        "eventType": "START",
        "eventTime": "2025-08-14T19:00:00.000Z",
        "job": {"namespace": "test", "name": "manual-test"},
        "run": {"runId": "manual-test-123"}
    }' \
    http://localhost:8080/api/v1/lineage)

echo "📄 Response: $RESPONSE"

echo ""
echo "📊 Endpoint Summary:"
echo "==================="
echo "🔗 Health Check:    curl http://localhost:8080/health"
echo "🔗 Lineage Endpoint: curl -X POST http://localhost:8080/api/v1/lineage"
echo "🔗 JAR Download:     curl http://localhost:8081/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar"
echo ""
echo "🔄 To see real-time logs:"
echo "docker logs -f openlineage-mock"
