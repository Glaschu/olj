package io.openlineage.demo;

import io.openlineage.client.OpenLineage;
import io.openlineage.client.OpenLineageClient;
import io.openlineage.client.transports.InsecureHttpConfig;
import io.openlineage.client.transports.InsecureHttpTransportBuilder;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;

/**
 * SSL Test Demo - Tests SSL bypass functionality
 * This demo tests connectivity to HTTPS endpoints with certificate issues
 */
public class SslTestDemo {
    public static void main(String[] args) {
        System.out.println("🔒 SSL Bypass Test - OpenLineage Insecure HTTP Transport");
        System.out.println("=========================================================");
        
        try {
            // Test 1: Regular HTTP (should work)
            System.out.println("\n1️⃣  Testing HTTP endpoint (should work):");
            testEndpoint("http://localhost:8080/api/v1/lineage", "HTTP");
            
            // Test 2: HTTPS endpoint that doesn't exist (would normally give SSL errors)
            System.out.println("\n2️⃣  Testing HTTPS endpoint with potential certificate issues:");
            testEndpoint("https://httpbin.org/post", "HTTPS-httpbin");
            
            // Test 3: Self-signed certificate (badssl.com test)
            System.out.println("\n3️⃣  Testing self-signed certificate with badssl.com:");
            testEndpoint("https://self-signed.badssl.com/", "BadSSL-SelfSigned");
            
            // Test 4: Expired certificate
            System.out.println("\n4️⃣  Testing expired certificate:");
            testEndpoint("https://expired.badssl.com/", "BadSSL-Expired");
            
            // Test 5: Wrong host certificate
            System.out.println("\n5️⃣  Testing wrong host certificate:");
            testEndpoint("https://wrong.host.badssl.com/", "BadSSL-WrongHost");
            
            System.out.println("\n✅ SSL bypass transport testing completed!");
            System.out.println("🎯 If all badssl.com tests passed, SSL bypass is working correctly");
            
        } catch (Exception e) {
            System.err.println("❌ Error in SSL test: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testEndpoint(String url, String testName) {
        try {
            System.out.println("🔗 Connecting to: " + url);
            
            // Create configuration
            InsecureHttpConfig config = new InsecureHttpConfig();
            config.setUrl(URI.create(url));
            
            // Optional: Add headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("User-Agent", "OpenLineage-Insecure-Transport-Test");
            config.setHeaders(headers);
            
            // Create transport builder and build the transport
            InsecureHttpTransportBuilder builder = new InsecureHttpTransportBuilder();
            
            // Create OpenLineage client using the insecure transport
            OpenLineageClient client = new OpenLineageClient(builder.build(config));
            
            // Create a simple lineage event
            OpenLineage ol = new OpenLineage(URI.create("https://test-producer.com"));
            
            OpenLineage.RunEvent runEvent = ol.newRunEventBuilder()
                .eventTime(ZonedDateTime.now())
                .eventType(OpenLineage.RunEvent.EventType.START)
                .run(ol.newRunBuilder()
                    .runId(UUID.randomUUID())
                    .build())
                .job(ol.newJobBuilder()
                    .namespace("ssl-test")
                    .name("ssl-bypass-test-job")
                    .build())
                .build();
            
            // Send the event
            client.emit(runEvent);
            
            System.out.println("✅ " + testName + " test successful - event sent!");
            
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (errorMsg.contains("connection refused") || errorMsg.contains("unknownhostexception")) {
                System.out.println("⚠️  " + testName + " endpoint not reachable (network issue)");
            } else if (errorMsg.contains("certificate") || errorMsg.contains("ssl") || 
                      errorMsg.contains("tls") || errorMsg.contains("pkix") || 
                      errorMsg.contains("trust") || errorMsg.contains("handshake")) {
                System.out.println("❌ " + testName + " SSL/Certificate error: " + e.getMessage());
                System.out.println("🔧 This suggests SSL bypass is NOT working - certificate validation failed");
            } else if (errorMsg.contains("404") || errorMsg.contains("405") || errorMsg.contains("method not allowed")) {
                System.out.println("✅ " + testName + " SSL bypass successful - reached server (got HTTP error, not SSL error)");
            } else {
                System.out.println("ℹ️  " + testName + " completed with: " + e.getMessage());
            }
        }
    }
}
