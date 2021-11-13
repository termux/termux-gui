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


send_msg(connection, '{"method":"newActivity", "params": {"overlay": true} }')
aid = (read_msg(connection))

f = io.open(sys.argv[1], "w")


#send_msg(connection, f'{{"method":"setTheme","params":{{"aid": "{aid}", "statusBarColor": {0xff003180},"colorPrimary": {0xffd1d1d1},"windowBackground": {0xffdedede},"textColor": {0xff000000},"colorAccent": {0xff003180} }} }}')

send_msg(connection, f'{{"method":"createLinearLayout", "params": {{"aid": "{aid}"}} }}')
root = read_msg(connection)


send_msg(connection, f'{{"method":"createEditText", "params": {{"aid": "{aid}", "parent": {root}, "text": "", "line": false }} }}')
tv = read_msg(connection)

send_msg(connection, f'{{"method":"setMargin", "params": {{"aid": "{aid}", "id": {tv}, "margin": 2 }} }}')

send_msg(connection, f'{{"method":"setWidth", "params": {{"aid": "{aid}", "id": {tv}, "width": 100 }} }}')
send_msg(connection, f'{{"method":"setHeight", "params": {{"aid": "{aid}", "id": {tv}, "height": 100 }} }}')

send_msg(connection, f'{{"method":"sendOverlayTouchEvent", "params": {{"aid": "{aid}", "send": true }} }}')



size = 100
scale = 0
try:
    while True:
        ev = read_msg(connection2)
        if ev["type"] == "overlayScale":
            if scale != 0:
                size = size + (scale - ev["value"])/5
            scale = ev["value"]
            send_msg(connection, f'{{"method":"setWidth", "params": {{"aid": "{aid}", "id": {tv}, "width": {int(size)} }} }}')
            send_msg(connection, f'{{"method":"setHeight", "params": {{"aid": "{aid}", "id": {tv}, "height": {int(size)} }} }}')
        if ev["type"] == "overlayTouch":
            #print("touch")
            if scale != 0:
                #print("drag")
                if ev["value"]["action"] == "up":
                    scale = 0
                else:
                    send_msg(connection, f'{{"method":"setPosition", "params": {{"aid": "{aid}", "x": {int(ev["value"]["x"])}, "y": {int(ev["value"]["y"])} }} }}')
                
except:
    try:
        print()
        send_msg(connection, f'{{"method":"getText", "params": {{"aid": "{aid}", "id": {tv} }} }}')
        f.write(read_msg(connection))
    except:
        pass
