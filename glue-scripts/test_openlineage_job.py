#!/usr/bin/env python3

import sys
from awsglue.transforms import *
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.job import Job
import boto3

# Parse job arguments
args = getResolvedOptions(sys.argv, ['JOB_NAME'])

# Initialize Spark and Glue contexts
sc = SparkContext.getOrCreate()
glueContext = GlueContext(sc)
spark = glueContext.spark_session
job = Job(glueContext)
job.init(args['JOB_NAME'], args)

print("🚀 Starting Glue job with OpenLineage insecure HTTP transport...")

# Check if OpenLineage is configured
spark_conf = spark.sparkContext.getConf()
print("📊 Spark Configuration:")
for key, value in spark_conf.getAll():
    if 'openlineage' in key.lower():
        print(f"  {key} = {value}")

# Create some sample data to trigger lineage events
print("📝 Creating sample data...")

# Create a simple DataFrame to trigger lineage collection
data = [
    ("Alice", 25, "Engineer"),
    ("Bob", 30, "Manager"), 
    ("Charlie", 35, "Analyst"),
    ("Diana", 28, "Developer")
]

columns = ["name", "age", "role"]
df = spark.createDataFrame(data, columns)

print("📋 Sample data created:")
df.show()

# Perform some transformations (this will generate lineage events)
print("🔄 Performing transformations...")

# Filter operation
filtered_df = df.filter(df.age > 26)
print("📊 Filtered data (age > 26):")
filtered_df.show()

# Aggregation operation  
from pyspark.sql.functions import avg, count
summary_df = df.groupBy("role").agg(
    count("*").alias("count"),
    avg("age").alias("avg_age")
)

print("📈 Summary by role:")
summary_df.show()

# Write to S3 (this will generate output lineage)
print("💾 Writing results to S3...")

output_path = "s3://glue-data-bucket/output/employee_summary/"

summary_df.write \
    .mode("overwrite") \
    .option("header", "true") \
    .csv(output_path)

print(f"✅ Data written to: {output_path}")

# Test direct HTTP call to verify our endpoint is reachable
print("🌐 Testing direct connection to lineage endpoint...")
try:
    import urllib3
    import json
    
    # Disable SSL warnings since we're using insecure transport
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    
    http = urllib3.PoolManager(cert_reqs='CERT_NONE')
    
    # Test endpoint
    test_event = {
        "eventType": "START",
        "eventTime": "2025-08-14T19:00:00.000Z",
        "job": {"namespace": "test", "name": "glue-test-job"},
        "run": {"runId": "test-run-123"}
    }
    
    response = http.request(
        'POST',
        'http://openlineage-mock/api/v1/lineage',
        body=json.dumps(test_event),
        headers={'Content-Type': 'application/json'}
    )
    
    print(f"📡 Test lineage call status: {response.status}")
    print(f"📄 Response: {response.data.decode('utf-8')}")
    
except Exception as e:
    print(f"⚠️  Error testing lineage endpoint: {str(e)}")

print("🎉 Glue job completed successfully!")
print("🔍 Check the mock lineage server logs for received events")

# Commit the job
job.commit()
