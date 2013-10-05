#!/usr/bin/python
import socket
import sys

# UDP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

# Bind the socket to the port
port = int(sys.argv[1]) if len(sys.argv) > 1 else 5556
server_address = ('0.0.0.0', port)
sock.bind(server_address)
print 'Listening on port %s' % port

while True:
    data, address = sock.recvfrom(4096)    
    #print 'Received %s bytes from %s' % (len(data), address)
    print data