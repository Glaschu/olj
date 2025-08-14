package io.openlineage.client.transports;

/**
 * Transport builder for creating insecure HTTP transports that bypass SSL certificate validation.
 * Uses a custom HTTP transport implementation based on Java's HttpURLConnection instead of Apache HttpClient.
 */
public class InsecureHttpTransportBuilder implements TransportBuilder {

    @Override
    public String getType() {
        return "http-insecure";
    }

    @Override
    public TransportConfig getConfig() {
        return new InsecureHttpConfig();
    }

    @Override
    public Transport build(TransportConfig config) {
        InsecureHttpConfig insecureConfig = (InsecureHttpConfig) config;
        
        // Return our custom InsecureHttpTransport that doesn't use Apache HttpClient
        return new InsecureHttpTransport(insecureConfig);
    }
}
