package io.openlineage.client.transports;

import org.junit.jupiter.api.Test;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that simulates how OpenLineage Spark discovers and loads transport builders.
 * This test verifies that the service discovery mechanism works as expected.
 */
public class TransportDiscoveryIntegrationTest {

    @Test
    public void testAllTransportDiscovery() {
        // Load all available transport builders via SPI
        ServiceLoader<TransportBuilder> serviceLoader = ServiceLoader.load(TransportBuilder.class);
        
        List<TransportBuilder> allBuilders = StreamSupport.stream(serviceLoader.spliterator(), false)
            .collect(Collectors.toList());
        
        // Print all discovered transports for debugging
        System.out.println("Discovered transport builders:");
        allBuilders.forEach(builder -> {
            System.out.println("  - Type: " + builder.getType() + ", Class: " + builder.getClass().getName());
        });
        
        // Verify our transport is in the list
        boolean hasInsecureHttp = allBuilders.stream()
            .anyMatch(builder -> "http-insecure".equals(builder.getType()));
        
        assertTrue(hasInsecureHttp, "http-insecure transport should be discoverable");
        
        // Verify we can get it by type (this is what OpenLineage Spark does)
        TransportBuilder insecureBuilder = findTransportByType("http-insecure");
        assertNotNull(insecureBuilder, "Should be able to find http-insecure transport by type");
        assertEquals("http-insecure", insecureBuilder.getType());
    }

    @Test
    public void testTransportTypeUniqueness() {
        // Verify that transport types are unique (no conflicts)
        ServiceLoader<TransportBuilder> serviceLoader = ServiceLoader.load(TransportBuilder.class);
        
        List<String> transportTypes = StreamSupport.stream(serviceLoader.spliterator(), false)
            .map(TransportBuilder::getType)
            .collect(Collectors.toList());
        
        // Check for duplicates
        long distinctCount = transportTypes.stream().distinct().count();
        assertEquals(transportTypes.size(), distinctCount, 
                    "Transport types should be unique. Found types: " + transportTypes);
    }

    @Test
    public void testSparkLikeTransportCreation() {
        // Simulate how OpenLineage Spark would create a transport
        // 1. Find transport by type
        TransportBuilder builder = findTransportByType("http-insecure");
        assertNotNull(builder, "Transport builder should be found");
        
        // 2. Get config template
        TransportConfig configTemplate = builder.getConfig();
        assertNotNull(configTemplate, "Config template should be available");
        assertTrue(configTemplate instanceof InsecureHttpConfig);
        
        // 3. Configure it (this is where Spark would apply spark.openlineage.transport.* properties)
        InsecureHttpConfig config = (InsecureHttpConfig) configTemplate;
        config.setUrl(java.net.URI.create("https://example.com/api/v1/lineage"));
        
        // 4. Build transport
        Transport transport = builder.build(config);
        assertNotNull(transport, "Transport should be built successfully");
        assertTrue(transport instanceof InsecureHttpTransport);
    }

    /**
     * Helper method that simulates how OpenLineage finds a transport by type
     */
    private TransportBuilder findTransportByType(String type) {
        ServiceLoader<TransportBuilder> serviceLoader = ServiceLoader.load(TransportBuilder.class);
        return StreamSupport.stream(serviceLoader.spliterator(), false)
            .filter(builder -> type.equals(builder.getType()))
            .findFirst()
            .orElse(null);
    }
}
