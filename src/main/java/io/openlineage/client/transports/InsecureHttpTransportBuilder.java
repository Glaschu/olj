package io.openlineage.client.transports;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
        
        // Set global SSL settings to ignore certificate validation
        setupInsecureSSL();
        
        // Create a regular HttpConfig from our InsecureHttpConfig
        HttpConfig httpConfig = new HttpConfig();
        httpConfig.setUrl(insecureConfig.getUrl());
        httpConfig.setHeaders(insecureConfig.getHeaders());
        httpConfig.setTimeoutInMillis(insecureConfig.getTimeoutInMillis());
        
        // Use standard HttpTransport - it will now use our insecure SSL settings
        return new HttpTransport(httpConfig);
    }
    
    private void setupInsecureSSL() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Trust all client certificates
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Trust all server certificates
                    }
                }
            };

            // Install the all-trusting trust manager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = (hostname, session) -> true;

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to setup insecure SSL", e);
        }
    }
}
