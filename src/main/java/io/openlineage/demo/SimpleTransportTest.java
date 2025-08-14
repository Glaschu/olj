package io.openlineage.demo;

import io.openlineage.client.OpenLineage;
import io.openlineage.client.OpenLineageClient;
import io.openlineage.client.transports.InsecureHttpConfig;
import io.openlineage.client.transports.InsecureHttpTransportBuilder;
import io.openlineage.client.transports.Transport;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Simple test to verify our InsecureHttpTransport is working correctly.
 */
public class SimpleTransportTest {
    
    public static void main(String[] args) {
        System.out.println("🧪 Simple Transport Test");
        System.out.println("========================");
        
        try {
            // Create configuration
            InsecureHttpConfig config = new InsecureHttpConfig();
            config.setUrl(URI.create("https://httpbin.org/post"));
            
            // Create transport using our builder
            InsecureHttpTransportBuilder builder = new InsecureHttpTransportBuilder();
            
            System.out.println("🔧 Building transport...");
            Transport transport = builder.build(config);
            System.out.println("✅ Transport built: " + transport.getClass().getName());
            
            // Create client using constructor (like SslTestDemo)
            System.out.println("🔧 Creating client...");
            OpenLineageClient client = new OpenLineageClient(transport);
            System.out.println("✅ Client created successfully");
            
            // Create a simple lineage event
            OpenLineage ol = new OpenLineage(URI.create("https://test-producer.com"));
            
            OpenLineage.RunEvent event = ol.newRunEventBuilder()
                .eventTime(ZonedDateTime.now())
                .eventType(OpenLineage.RunEvent.EventType.START)
                .run(ol.newRunBuilder()
                    .runId(UUID.randomUUID())
                    .build())
                .job(ol.newJobBuilder()
                    .namespace("simple-test")
                    .name("simple-transport-test")
                    .build())
                .build();
            
            System.out.println("📤 Sending lineage event...");
            client.emit(event);
            System.out.println("✅ Event sent successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
