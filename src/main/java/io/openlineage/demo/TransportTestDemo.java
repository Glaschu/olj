package io.openlineage.demo;

import io.openlineage.client.OpenLineage;
import io.openlineage.client.OpenLineageClient;
import io.openlineage.client.transports.InsecureHttpConfig;
import io.openlineage.client.transports.InsecureHttpTransportBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Test demo specifically for verifying our insecure HTTP transport.
 * This will send lineage events to a local server.
 */
public class TransportTestDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 OpenLineage Insecure HTTP Transport Test");
        System.out.println("============================================");
        
        try {
            // Create configuration for our insecure HTTP transport
            InsecureHttpConfig config = new InsecureHttpConfig();
            config.setUrl(URI.create("https://httpbin.org/post"));
            config.setTimeout(Duration.ofSeconds(5)); // 5 second timeout
            
            System.out.println("📡 Transport Configuration:");
            System.out.println("   URL: " + config.getUrl());
            System.out.println("   Timeout: " + config.getTimeoutInMillis() + "ms");
            System.out.println("   Type: http-insecure");
            
            // Create transport and client
            InsecureHttpTransportBuilder builder = new InsecureHttpTransportBuilder();
            
            // Create client with our transport
            OpenLineageClient client = OpenLineageClient.builder()
                .transport(builder.build(config))
                .build();
            
            // Create OpenLineage instance
            OpenLineage ol = new OpenLineage(URI.create("http://example.com"));
            
            // Create a lineage event
            OpenLineage.RunEvent event = ol.newRunEventBuilder()
                .eventTime(ZonedDateTime.now())
                .eventType(OpenLineage.RunEvent.EventType.START)
                .run(ol.newRunBuilder()
                    .runId(UUID.randomUUID())
                    .build())
                .job(ol.newJobBuilder()
                    .namespace("test-namespace")
                    .name("transport-test-job")
                    .build())
                .build();
            
            System.out.println("\n📤 Sending lineage event...");
            System.out.println("   Event Type: " + event.getEventType());
            System.out.println("   Job: " + event.getJob().getNamespace() + "/" + event.getJob().getName());
            System.out.println("   Run ID: " + event.getRun().getRunId());
            
            // Send the event
            client.emit(event);
            
            System.out.println("\n✅ Successfully sent lineage event using insecure HTTP transport!");
            System.out.println("🎯 Check your local server logs to see the received event");
            
        } catch (Exception e) {
            System.err.println("\n❌ Error sending lineage event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
