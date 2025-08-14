package io.openlineage.demo;

import io.openlineage.client.transports.InsecureHttpTransportBuilder;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DirectSslTestDemo {
    public static void main(String[] args) {
        System.out.println("🔒 Direct SSL Bypass Test");
        System.out.println("========================");
        
        try {
            // Create the transport builder (this will call setupGlobalInsecureSSL)
            InsecureHttpTransportBuilder builder = new InsecureHttpTransportBuilder();
            // Trigger the SSL setup by creating a transport
            builder.build(builder.getConfig());
            
            System.out.println("✅ SSL bypass setup completed");
            
            // Test direct HTTPS connection to a bad SSL site
            System.out.println("\n🔗 Testing direct connection to https://self-signed.badssl.com/");
            
            URL url = new URL("https://self-signed.badssl.com/");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            System.out.println("✅ SUCCESS! Response code: " + responseCode);
            System.out.println("🎯 SSL bypass is working for direct connections!");
            
            // Read first few lines of response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = reader.readLine();
            if (line != null && line.contains("html")) {
                System.out.println("📄 Received HTML response - connection successful");
            }
            reader.close();
            
        } catch (javax.net.ssl.SSLHandshakeException e) {
            System.out.println("❌ SSL handshake failed: " + e.getMessage());
            System.out.println("🔧 SSL bypass is NOT working for direct connections");
        } catch (Exception e) {
            System.out.println("ℹ️  Other error (may still indicate SSL bypass worked): " + e.getMessage());
        }
    }
}
