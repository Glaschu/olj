#!/usr/bin/env python3
"""
Mock OpenLineage server to receive and display lineage events.
"""
import json
import sys
from http.server import HTTPServer, BaseHTTPRequestHandler
from datetime import datetime

class LineageHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path == "/api/v1/lineage":
            # Read the request body
            content_length = int(self.headers.get('Content-Length', 0))
            post_data = self.rfile.read(content_length)
            
            try:
                # Parse the JSON lineage event
                event = json.loads(post_data.decode('utf-8'))
                
                # Display the received event
                print(f"\n📨 Received OpenLineage Event at {datetime.now().strftime('%H:%M:%S')}")
                print("=" * 60)
                print(f"📍 Event Type: {event.get('eventType', 'unknown')}")
                print(f"🚀 Job: {event.get('job', {}).get('namespace', 'unknown')}/{event.get('job', {}).get('name', 'unknown')}")
                print(f"🔄 Run ID: {event.get('run', {}).get('runId', 'unknown')}")
                print(f"⏰ Event Time: {event.get('eventTime', 'unknown')}")
                print(f"📦 Producer: {event.get('producer', 'unknown')}")
                
                # Show inputs/outputs if present
                if 'inputs' in event:
                    print(f"📥 Inputs: {len(event['inputs'])} dataset(s)")
                if 'outputs' in event:
                    print(f"📤 Outputs: {len(event['outputs'])} dataset(s)")
                
                print("=" * 60)
                
                # Send successful response
                self.send_response(201)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                response = {"status": "received", "timestamp": datetime.now().isoformat()}
                self.wfile.write(json.dumps(response).encode())
                
            except json.JSONDecodeError as e:
                print(f"❌ Error parsing JSON: {e}")
                self.send_response(400)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                error_response = {"error": "Invalid JSON", "message": str(e)}
                self.wfile.write(json.dumps(error_response).encode())
        else:
            # Handle other paths
            self.send_response(404)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            error_response = {"error": "Not found", "path": self.path}
            self.wfile.write(json.dumps(error_response).encode())
    
    def log_message(self, format, *args):
        # Suppress default logging to keep output clean
        pass

def run_server(port=8080):
    server_address = ('', port)
    httpd = HTTPServer(server_address, LineageHandler)
    print(f"🚀 Mock OpenLineage Server starting on port {port}")
    print(f"📍 Endpoint: http://localhost:{port}/api/v1/lineage")
    print("🔄 Waiting for lineage events...")
    print("=" * 60)
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n👋 Server shutting down...")
        httpd.shutdown()

if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    run_server(port)
