package io.openlineage.client.transports;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public class InsecureHttpConfig implements TransportConfig {
    private URI url;
    private Duration timeout;
    private Map<String, String> headers;
    
    public URI getUrl() {
        return url;
    }
    
    public void setUrl(URI url) {
        this.url = url;
    }
    
    public Duration getTimeout() {
        return timeout;
    }
    
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public Integer getTimeoutInMillis() {
        return timeout != null ? (int) timeout.toMillis() : 5000; // Default 5 seconds
    }
}
