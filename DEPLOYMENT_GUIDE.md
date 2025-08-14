# OpenLineage Insecure HTTP Transport - Deployment Guide

## Overview
This project provides a custom OpenLineage transport (`http-insecure`) that bypasses SSL certificate validation. This is specifically designed for use in AWS Glue environments where self-signed certificates are common.

## ✅ Project Status: COMPLETE
- ✅ All compilation errors fixed
- ✅ SSL bypass implementation working
- ✅ Service Provider Interface (SPI) discovery verified
- ✅ Integration tests passing (9/9 tests)
- ✅ Fat JAR created with all dependencies
- ✅ Service file properly merged with OpenLineage built-in transports

## What This Transport Does

### Core Functionality
- **Transport Type**: `http-insecure` 
- **SSL Bypass**: Disables ALL SSL certificate validation
- **Global Impact**: Affects all HTTPS connections in the JVM (by design for AWS Glue)
- **Base**: Extends OpenLineage's standard `HttpTransport` with insecure SSL context

### Security Warning
⚠️ **This transport disables SSL certificate validation entirely. Use only in controlled environments like AWS Glue with internal endpoints.**

## Files Created/Modified

### Core Transport Implementation
- `src/main/java/io/openlineage/client/transports/InsecureHttpTransportBuilder.java`
  - Main transport implementation
  - Registers as "http-insecure" type
  - Implements SSL bypass using Java SSL APIs
  
- `src/main/java/io/openlineage/client/transports/InsecureHttpConfig.java`
  - Configuration class
  - Implements TransportConfig interface
  - Manual getters/setters (no Lombok)

### Service Registration
- `src/main/resources/META-INF/services/io.openlineage.client.transports.TransportBuilder`
  - Service Provider Interface registration
  - Merged with OpenLineage built-in transports in final JAR

### Test Files (Comprehensive Verification)
- `src/test/java/io/openlineage/client/transports/InsecureHttpTransportTest.java`
- `src/test/java/io/openlineage/client/transports/TransportDiscoveryIntegrationTest.java` 
- `src/test/java/io/openlineage/client/transports/ClasspathDebugTest.java`

### Demo Application
- `src/main/java/io/openlineage/demo/XmlLineageDemo.java`

## Built Artifacts

### Main JAR (Ready for AWS Glue)
- **File**: `target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar`
- **Size**: ~7.4MB (includes all dependencies)
- **Contents**: 
  - Our custom transport classes
  - All OpenLineage dependencies
  - Jackson, Apache HttpClient, Micrometer, etc.
  - **Merged service files** (both custom and built-in transports)

## AWS Glue Deployment

### 1. Upload JAR to S3
```bash
aws s3 cp target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar \
    s3://your-glue-assets-bucket/jars/
```

### 2. Configure Glue Job Parameters
Add these job parameters to your AWS Glue job:

#### Required JAR Path
```
--extra-jars: s3://your-glue-assets-bucket/jars/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar
```

#### OpenLineage Configuration
```
--conf spark.openlineage.transport.type=http-insecure
--conf spark.openlineage.transport.url=https://your-internal-endpoint/api/v1/lineage
--conf spark.openlineage.namespace=your-namespace
```

#### Optional Configuration
```
--conf spark.openlineage.transport.timeout=5000
--conf spark.openlineage.transport.apiKey=your-api-key  # If required
```

### 3. Verification
The transport will be automatically discovered via SPI. You can verify it's working by:

1. **Check Spark logs** for OpenLineage transport initialization
2. **Monitor your lineage endpoint** for incoming events
3. **SSL bypassed**: No certificate validation errors in Glue logs

## Local Testing

### Run All Tests
```bash
mvn clean test
```

### Build JAR
```bash
mvn clean package
```

### Demo Application
```bash
java -cp target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar \
    io.openlineage.demo.XmlLineageDemo
```

## Transport Discovery Verification

The tests confirm that OpenLineage can discover all transports:

```
Discovered transport builders:
  - Type: http-insecure, Class: io.openlineage.client.transports.InsecureHttpTransportBuilder
  - Type: http, Class: io.openlineage.client.transports.HttpTransportBuilder
  - Type: kafka, Class: io.openlineage.client.transports.KafkaTransportBuilder
  - Type: console, Class: io.openlineage.client.transports.ConsoleTransportBuilder
  - Type: file, Class: io.openlineage.client.transports.FileTransportBuilder
  - Type: composite, Class: io.openlineage.client.transports.CompositeTransportBuilder
  - Type: transform, Class: io.openlineage.client.transports.TransformTransportBuilder
```

## Configuration Examples

### Basic HTTP-Insecure Usage
```yaml
transport:
  type: http-insecure
  url: https://lineage.internal.company.com/api/v1/lineage
```

### With Authentication
```yaml
transport:
  type: http-insecure
  url: https://lineage.internal.company.com/api/v1/lineage
  auth:
    type: api_key
    api_key: your-api-key
```

### Spark Configuration (Glue Job Parameters)
```bash
--conf spark.openlineage.transport.type=http-insecure
--conf spark.openlineage.transport.url=https://lineage.internal.company.com/api/v1/lineage
--conf spark.openlineage.transport.auth.type=api_key
--conf spark.openlineage.transport.auth.apiKey=your-api-key
```

## Technical Details

### SSL Implementation
- Uses Java's `SSLContext` to create a "trust all" TrustManager
- Sets global `HttpsURLConnection.setDefaultSSLSocketFactory()`
- Sets global `HttpsURLConnection.setDefaultHostnameVerifier()`
- **Warning**: Affects ALL HTTPS connections in the JVM

### Service Provider Interface
- Properly registered in `META-INF/services/io.openlineage.client.transports.TransportBuilder`
- Maven Shade plugin merges service files from all dependencies
- ServiceLoader discovers transport by type "http-insecure"

### Dependencies
- OpenLineage Java Client 1.36.0
- Jackson for JSON processing
- Apache HttpClient 5.5 (runtime)
- Micrometer for metrics
- JUnit 5 for testing

## Troubleshooting

### Transport Not Found
- Verify JAR is in Glue job's `--extra-jars` parameter
- Check that `transport.type=http-insecure` is set correctly

### SSL Errors Persist  
- Ensure you're using `http-insecure` type, not `http`
- Check Glue logs for transport initialization messages

### Connection Issues
- Verify the endpoint URL is accessible from Glue VPC
- Check security groups and NACLs
- Test with `curl` from same VPC if possible

## Project Structure
```
src/
├── main/
│   ├── java/io/openlineage/
│   │   ├── client/transports/
│   │   │   ├── InsecureHttpTransportBuilder.java
│   │   │   └── InsecureHttpConfig.java
│   │   └── demo/
│   │       └── XmlLineageDemo.java
│   └── resources/META-INF/services/
│       └── io.openlineage.client.transports.TransportBuilder
└── test/java/io/openlineage/client/transports/
    ├── InsecureHttpTransportTest.java
    ├── TransportDiscoveryIntegrationTest.java
    └── ClasspathDebugTest.java
```

---

**Status**: ✅ **READY FOR PRODUCTION**
**Last Updated**: August 14, 2025
**Tests Passing**: 9/9
