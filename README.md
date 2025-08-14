# OpenLineage Insecure HTTP Transport

This project provides an OpenLineage transport that sends events over HTTP without SSL certificate validation. This transport **actually disables SSL certificate validation and hostname verification**, making it suitable for development environments with self-signed certificates or internal HTTPS endpoints.

## Features

- **True SSL certificate validation bypass** - disables all certificate checks
- **Hostname verification bypass** - accepts any hostname
- Compatible with OpenLineage Java client 1.36.0
- Configurable timeout and headers
- Service provider interface (SPI) integration
- AWS Glue compatible

## How It Works

This transport uses Java's built-in SSL capabilities to:

1. **Create a trust manager that accepts all certificates** (including self-signed and expired ones)
2. **Install a hostname verifier that accepts all hostnames** 
3. **Set these as global defaults** for HTTPS connections used by the underlying HTTP transport

This means it will work with:
- Self-signed certificates
- Expired certificates  
- Certificates with mismatched hostnames
- Any HTTPS endpoint regardless of certificate validity

## Usage

### Local Testing with LocalStack

For local development and testing, you can use the included LocalStack setup:

```bash
# Complete demo with LocalStack
./demo.sh

# Or run individual steps:
./setup_localstack.sh    # Setup LocalStack + Glue
./run_glue_job.sh        # Run test job
./view_lineage_events.sh # Check results
```

See [LOCALSTACK_README.md](LOCALSTACK_README.md) for detailed local testing instructions.

### AWS Glue Integration

You can use this transport in AWS Glue by following these steps:

1. **Upload JAR to S3:**
   ```bash
   aws s3 cp target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar s3://your-bucket/jars/
   ```

2. **Configure AWS Glue Job:**

   **Dependent JARs path:**
   ```
   s3://your-bucket/jars/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar,https://repo1.maven.org/maven2/io/openlineage/openlineage-spark_2.12/1.36.0/openlineage-spark_2.12-1.36.0.jar
   ```

   **Job parameters:**
   ```
   --conf spark.extraListeners=io.openlineage.spark.agent.OpenLineageSparkListener --conf spark.openlineage.transport.type=http-insecure --conf spark.openlineage.transport.url=https://your-endpoint.com --conf spark.openlineage.transport.endpoint=/api/v1/lineage --conf spark.openlineage.transport.headers.Authorization=Bearer your-token
   ```

   **User Jars First:** `true`

### Maven Dependency

Add the JAR to your classpath or install it in your local Maven repository:

```bash
mvn install:install-file -Dfile=target/openlineage-transport-http-insecure-1.0-SNAPSHOT.jar \
                         -DgroupId=io.openlineage \
                         -DartifactId=openlineage-transport-http-insecure \
                         -Dversion=1.0-SNAPSHOT \
                         -Dpackaging=jar
```

### Programmatic Usage

```java
import io.openlineage.client.OpenLineageClient;
import io.openlineage.client.transports.InsecureHttpConfig;
import io.openlineage.client.transports.InsecureHttpTransportBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

// Create configuration
InsecureHttpConfig config = new InsecureHttpConfig();
config.setUrl(URI.create("https://localhost:8080/api/v1/lineage"));

// Optional: Add headers
Map<String, String> headers = new HashMap<>();
headers.put("Authorization", "Bearer your-token");
config.setHeaders(headers);

// Create transport and client
InsecureHttpTransportBuilder builder = new InsecureHttpTransportBuilder();
OpenLineageClient client = new OpenLineageClient(builder.build(config));

// Use the client to emit events
// client.emit(runEvent);
```

### Configuration via YAML

You can also use this transport through OpenLineage's configuration system by specifying the transport type as `http-insecure`:

```yaml
transport:
  type: http-insecure
  url: https://localhost:8080/api/v1/lineage
  headers:
    Authorization: Bearer your-token
  timeout: PT30S
```

## Configuration Properties

| Property | Type | Description | Required |
|----------|------|-------------|----------|
| `url` | URI | The endpoint URL for the OpenLineage service | Yes |
| `headers` | Map<String, String> | HTTP headers to include with requests | No |
| `timeout` | Duration | Request timeout | No |

## Security Warning

⚠️ **Critical Security Warning**: This transport **completely disables SSL certificate validation and hostname verification**. This makes your application vulnerable to man-in-the-middle attacks. 

**Only use this transport:**
- In development environments
- On trusted networks
- With internal services where SSL validation is problematic
- **Never in production with external or untrusted endpoints**

For production environments, always use proper SSL certificates and the standard `http` transport type.

## Building

```bash
mvn clean package
```

This will create a shaded JAR with all dependencies included.

## Transport Type

This transport registers with the type identifier: `http-insecure`

## Compatibility

- Java 8+
- OpenLineage Java client 1.36.0
- AWS Glue (Spark with Scala 2.12)
