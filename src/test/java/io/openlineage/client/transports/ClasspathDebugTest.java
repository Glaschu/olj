package io.openlineage.client.transports;

import org.junit.jupiter.api.Test;
import java.net.URL;
import java.util.Enumeration;
import java.io.IOException;

/**
 * Debug test to understand classpath loading
 */
public class ClasspathDebugTest {

    @Test
    public void debugClasspathServiceFiles() throws IOException {
        System.out.println("=== Debugging SPI Service Files ===");
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        // Find all service files
        Enumeration<URL> serviceFiles = classLoader.getResources("META-INF/services/io.openlineage.client.transports.TransportBuilder");
        
        System.out.println("Found service files:");
        int fileCount = 0;
        while (serviceFiles.hasMoreElements()) {
            URL serviceFile = serviceFiles.nextElement();
            fileCount++;
            System.out.println("  " + fileCount + ". " + serviceFile);
            
            // Read contents
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(serviceFile.openStream()))) {
                System.out.println("    Contents:");
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        System.out.println("      " + line);
                    }
                }
            }
        }
        
        System.out.println("Total service files found: " + fileCount);
        
        // Test ServiceLoader directly
        System.out.println("\n=== Testing ServiceLoader Directly ===");
        java.util.ServiceLoader<TransportBuilder> serviceLoader = 
            java.util.ServiceLoader.load(TransportBuilder.class);
        
        System.out.println("ServiceLoader found:");
        for (TransportBuilder builder : serviceLoader) {
            System.out.println("  - " + builder.getType() + " (" + builder.getClass().getName() + ")");
        }
        
        // Check if our class is loadable
        System.out.println("\n=== Testing Class Loading ===");
        try {
            Class<?> clazz = classLoader.loadClass("io.openlineage.client.transports.InsecureHttpTransportBuilder");
            System.out.println("✓ InsecureHttpTransportBuilder is loadable: " + clazz.getName());
        } catch (ClassNotFoundException e) {
            System.out.println("✗ InsecureHttpTransportBuilder is NOT loadable: " + e.getMessage());
        }
        
        // Test built-in transport
        try {
            Class<?> clazz = classLoader.loadClass("io.openlineage.client.transports.HttpTransportBuilder");
            System.out.println("✓ HttpTransportBuilder is loadable: " + clazz.getName());
        } catch (ClassNotFoundException e) {
            System.out.println("✗ HttpTransportBuilder is NOT loadable: " + e.getMessage());
        }
    }
}
