#!/bin/bash

# Demo script - Complete workflow demonstration

set -e

echo "🎬 OpenLineage Insecure HTTP Transport - Complete Demo"
echo "====================================================="
echo ""

echo "📋 This demo will:"
echo "  1. ✅ Show current project status"
echo "  2. 🐳 Start LocalStack environment"  
echo "  3. 🚀 Run a Glue job with our transport"
echo "  4. 📊 Show lineage events received"
echo "  5. 🧹 Clean up resources"
echo ""

read -p "🤔 Continue with demo? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "👋 Demo cancelled"
    exit 0
fi

echo ""
echo "1️⃣  Current Status"
echo "=================="
./status.sh

echo ""
echo "2️⃣  Starting LocalStack Environment"
echo "===================================="
./setup_localstack.sh

echo ""
echo "⏳ Waiting 5 seconds for services to fully initialize..."
sleep 5

echo ""
echo "3️⃣  Running Glue Job with Insecure HTTP Transport"
echo "================================================="
./run_glue_job.sh

echo ""
echo "4️⃣  Checking Results"
echo "==================="
echo ""
echo "📋 Job Logs:"
./check_logs.sh

echo ""
echo "📨 Lineage Events:"
./view_lineage_events.sh

echo ""
echo "5️⃣  Demo Summary"
echo "================"

# Check if lineage events were received
EVENTS_RECEIVED=$(docker exec openlineage-mock cat /var/log/nginx/lineage_events.log 2>/dev/null | wc -l || echo "0")

if [ "$EVENTS_RECEIVED" -gt 0 ]; then
    echo "🎉 SUCCESS! Received $EVENTS_RECEIVED lineage events"
    echo "✅ The insecure HTTP transport is working correctly"
else
    echo "⚠️  No lineage events received"
    echo "💡 Check the logs above for potential issues"
fi

echo ""
echo "📊 Final Status:"
./status.sh

echo ""
read -p "🧹 Clean up and stop services? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🛑 Stopping services..."
    docker-compose down
    echo "✅ Cleanup completed"
else
    echo "💡 Services are still running. Stop them with: docker-compose down"
fi

echo ""
echo "🎯 Demo completed!"
echo ""
echo "📚 Next steps:"
echo "  - Deploy the JAR to real AWS Glue using DEPLOYMENT_GUIDE.md"
echo "  - Customize the transport for your specific needs"
echo "  - Check the comprehensive documentation in LOCALSTACK_README.md"
