package io.openlineage.client.transports;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the InsecureHttpTransport can be discovered and loaded
 * by OpenLineage's service provider interface (SPI) mechanism.
 */
public class InsecureHttpTransportTest {

    @Test
    public void testTransportServiceDiscovery() {
        // Test that our transport can be discovered via SPI
        ServiceLoader<TransportBuilder> serviceLoader = ServiceLoader.load(TransportBuilder.class);
        
        // Find our specific transport
        TransportBuilder insecureBuilder = StreamSupport.stream(serviceLoader.spliterator(), false)
            .filter(builder -> "http-insecure".equals(builder.getType()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(insecureBuilder, "InsecureHttpTransportBuilder should be discoverable via SPI");
        assertTrue(insecureBuilder instanceof InsecureHttpTransportBuilder, 
                  "Found builder should be instance of InsecureHttpTransportBuilder");
    }

    @Test
    public void testTransportConfiguration() {
        InsecureHttpTransportBuilder builder = new InsecureHttpTransportBuilder();
        
        // Test transport type
        assertEquals("http-insecure", builder.getType());
        
        // Test config creation
        TransportConfig config = builder.getConfig();
        assertNotNull(config);
        assertTrue(config instanceof InsecureHttpConfig);
    }

    @Test
    public void testTransportBuild() {
        InsecureHttpTransportBuilder builder = new InsecureHttpTransportBuilder();
        
        // Create configuration
        InsecureHttpConfig config = new InsecureHttpConfig();
        config.setUrl(URI.create("https://test-endpoint.example.com/api/v1/lineage"));
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        config.setHeaders(headers);
        
        // Build transport
        Transport transport = builder.build(config);
        assertNotNull(transport);
        assertTrue(transport instanceof HttpTransport);
    }

    @Test
    public void testInsecureHttpConfig() {
        InsecureHttpConfig config = new InsecureHttpConfig();
        
        // Test URL setting
        URI testUrl = URI.create("https://localhost:8080/lineage");
        config.setUrl(testUrl);
        assertEquals(testUrl, config.getUrl());
        
        // Test headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer test");
        config.setHeaders(headers);
        assertEquals(headers, config.getHeaders());
        
        // Test timeout
        java.time.Duration timeout = java.time.Duration.ofSeconds(30);
        config.setTimeout(timeout);
        assertEquals(timeout, config.getTimeout());
        assertEquals(30000, config.getTimeoutInMillis().intValue());
    }

    @Test
    public void testSSLContextModification() {
        // This test verifies that SSL context is modified when transport is built
        InsecureHttpTransportBuilder builder = new InsecureHttpTransportBuilder();
        InsecureHttpConfig config = new InsecureHttpConfig();
        config.setUrl(URI.create("https://self-signed.badssl.com/"));
        
        // Build the transport - this should modify global SSL settings
        assertDoesNotThrow(() -> {
            Transport transport = builder.build(config);
            assertNotNull(transport);
        });
        
        // Note: Testing actual SSL bypass would require making real HTTPS calls
        // which is beyond the scope of unit tests, but this verifies the transport builds successfully
    }
}
