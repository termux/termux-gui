import socket
import sys
import os
import struct
import time
import json
import base64
import mmap
import array
import io
import subprocess



def read_msg(s):
    b = b''
    togo = 4
    while togo > 0:
        read = s.recv(togo)
        b = b + read
        togo = togo - len(read)
    togo = int.from_bytes(b, "big")
    b = b''
    while togo > 0:
        read = s.recv(togo)
        b = b + read
        togo = togo - len(read)
    return json.loads(b.decode("utf-8"))
    
def send_msg(s, msg):
    m = bytes(msg, "utf-8")
    connection.sendall((len(m)).to_bytes(4,"big"))
    connection.sendall(m)

def return_msg(s, msg):
    send_msg(s, msg)
    return read_msg(s)


sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
sock.bind("\0test")
sock2 = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
sock2.bind("\0test2")

sock.listen(1)
sock2.listen(1)

os.system("am broadcast --user 0 -n com.termux.gui/.GUIReceiver --es mainSocket test --es eventSocket test2 >/dev/null 2>&1")


connection, _ = sock.accept()
connection2, client_address2 = sock2.accept()
connection.sendall(b'\x01')

ret = b''
while len(ret) == 0:
    ret = ret + connection.recv(1)

if len(sys.argv) != 2 or sys.argv[1] == None:
    sys.exit(1)



send_msg(connection, f'{{"method": "bindWidget", "params": {{"wid": {sys.argv[1]} }} }}')

send_msg(connection, f'{{"method":"setTheme","params":{{"wid": "{sys.argv[1]}", "statusBarColor": {0xff003180},"colorPrimary": {0xffd1d1d1},"windowBackground": {0x60dedede},"textColor": {0xff000000},"colorAccent": {0xff003180} }} }}')

while True:
    out = subprocess.check_output(["ps", "-e", "--no-headers", "-o", "%C %x %c"]).decode("ascii")
    #print(out)
    tv = return_msg(connection, f'{{"method": "createTextView", "params": {{"wid": {sys.argv[1]}, "text": "{out}" }} }}')
    
    
    send_msg(connection, f'{{"method": "blitWidget", "params": {{"wid": {sys.argv[1]} }} }}')
    send_msg(connection, f'{{"method": "clearWidget", "params": {{"wid": {sys.argv[1]} }} }}')
    time.sleep(1)


