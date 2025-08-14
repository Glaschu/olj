package io.openlineage.client.transports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.openlineage.client.OpenLineage;
import io.openlineage.client.OpenLineageClientException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Custom HTTP transport that bypasses SSL certificate validation.
 */
public class InsecureHttpTransport extends Transport {
    
    private final InsecureHttpConfig config;
    private final ObjectMapper objectMapper;
    
    public InsecureHttpTransport(InsecureHttpConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        setupInsecureSSL();
    }
    
    @Override
    public void emit(OpenLineage.RunEvent event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            sendHttpRequest(jsonPayload);
        } catch (JsonProcessingException e) {
            throw new OpenLineageClientException("Failed to serialize event to JSON", e);
        } catch (IOException e) {
            throw new OpenLineageClientException("Failed to send HTTP request", e);
        }
    }
    
    @Override
    public void emit(OpenLineage.DatasetEvent event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            sendHttpRequest(jsonPayload);
        } catch (JsonProcessingException e) {
            throw new OpenLineageClientException("Failed to serialize event to JSON", e);
        } catch (IOException e) {
            throw new OpenLineageClientException("Failed to send HTTP request", e);
        }
    }
    
    @Override
    public void emit(OpenLineage.JobEvent event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            sendHttpRequest(jsonPayload);
        } catch (JsonProcessingException e) {
            throw new OpenLineageClientException("Failed to serialize event to JSON", e);
        } catch (IOException e) {
            throw new OpenLineageClientException("Failed to send HTTP request", e);
        }
    }
    
    private void sendHttpRequest(String jsonPayload) throws IOException {
        URL url = config.getUrl().toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(config.getTimeoutInMillis());
            connection.setReadTimeout(config.getTimeoutInMillis());
            
            if (config.getHeaders() != null) {
                for (Map.Entry<String, String> header : config.getHeaders().entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            
            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
                outputStream.flush();
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("HTTP request failed with response code: " + responseCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    private void setupInsecureSSL() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to setup insecure SSL context", e);
        }
    }
}
