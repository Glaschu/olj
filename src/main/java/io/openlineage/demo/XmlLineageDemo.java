package io.openlineage.demo;

import io.openlineage.client.OpenLineage;
import io.openlineage.client.OpenLineageClient;
import io.openlineage.client.transports.InsecureHttpConfig;
import io.openlineage.client.transports.InsecureHttpTransportBuilder;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Demo showing how to use the InsecureHttpTransport for OpenLineage events.
 * 
 * This transport disables SSL certificate validation, making it useful for
 * development environments with self-signed certificates.
 */
public class XmlLineageDemo {
    
    public static void main(String[] args) {
        try {
            // Create configuration for insecure HTTP transport
            InsecureHttpConfig config = new InsecureHttpConfig();
            config.setUrl(URI.create("https://localhost:8080/api/v1/lineage")); // Example endpoint
            
            // Optional: Add headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer your-token");
            headers.put("Content-Type", "application/json");
            config.setHeaders(headers);
            
            // Create transport builder and build the transport
            InsecureHttpTransportBuilder builder = new InsecureHttpTransportBuilder();
            
            // Create OpenLineage client using the insecure transport
            OpenLineageClient client = new OpenLineageClient(builder.build(config));
            
            // Create a simple lineage event
            OpenLineage ol = new OpenLineage(URI.create("https://example.com/producer"));
            
            OpenLineage.RunEvent runEvent = ol.newRunEventBuilder()
                .eventTime(ZonedDateTime.now())
                .eventType(OpenLineage.RunEvent.EventType.START)
                .run(ol.newRunBuilder()
                    .runId(UUID.randomUUID())
                    .build())
                .job(ol.newJobBuilder()
                    .namespace("example-namespace")
                    .name("example-job")
                    .build())
                .build();
            
            // Send the event (this would normally send to your OpenLineage endpoint)
            client.emit(runEvent);
            
            System.out.println("Successfully sent lineage event using insecure HTTP transport!");
            
        } catch (Exception e) {
            System.err.println("Error in demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}