#!/usr/bin/env python3

import http.server
import ssl
import threading
import time
import os

# Create a simple HTTPS server with self-signed certificate
class SimpleHTTPSServer:
    def __init__(self, port=8443):
        self.port = port
        self.server = None
        
    def create_self_signed_cert(self):
        """Create a self-signed certificate for testing"""
        import subprocess
        
        # Create self-signed certificate
        subprocess.run([
            'openssl', 'req', '-x509', '-newkey', 'rsa:4096', '-keyout', 'key.pem', 
            '-out', 'cert.pem', '-days', '1', '-nodes', '-subj', 
            '/C=US/ST=Test/L=Test/O=Test/CN=localhost'
        ], capture_output=True)
        
    def start(self):
        """Start the HTTPS server"""
        handler = http.server.SimpleHTTPRequestHandler
        
        class LineageHandler(handler):
            def do_POST(self):
                if self.path == '/api/v1/lineage':
                    content_length = int(self.headers.get('Content-Length', 0))
                    post_data = self.rfile.read(content_length)
                    
                    self.send_response(201)
                    self.send_header('Content-type', 'application/json')
                    self.end_headers()
                    response = '{"status": "received", "ssl": "self-signed"}'
                    self.wfile.write(response.encode())
                    print(f"Received lineage event: {post_data.decode()[:100]}...")
                else:
                    self.send_response(404)
                    self.end_headers()
                    
        self.server = http.server.HTTPServer(('localhost', self.port), LineageHandler)
        
        # Create SSL context with self-signed cert
        if not os.path.exists('cert.pem'):
            self.create_self_signed_cert()
            
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.load_cert_chain('cert.pem', 'key.pem')
        self.server.socket = context.wrap_socket(self.server.socket, server_side=True)
        
        print(f"Starting HTTPS server on https://localhost:{self.port}")
        print("This server has a SELF-SIGNED certificate (would normally cause SSL errors)")
        
        # Start server in background thread
        def serve():
            self.server.serve_forever()
            
        self.thread = threading.Thread(target=serve, daemon=True)
        self.thread.start()
        
    def stop(self):
        if self.server:
            self.server.shutdown()

if __name__ == "__main__":
    server = SimpleHTTPSServer()
    try:
        server.start()
        time.sleep(60)  # Run for 1 minute
    except KeyboardInterrupt:
        pass
    finally:
        server.stop()
        # Clean up certificates
        for f in ['cert.pem', 'key.pem']:
            if os.path.exists(f):
                os.remove(f)
