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


send_msg(connection, '{"method":"newActivity", "params": {"lockscreen": true} }')
aid, tid = (read_msg(connection))

send_msg(connection, f'{{"method":"setTaskDescription", "params": {{"aid": "{aid}", "label": "Lockscreen Notes" }} }}')
send_msg(connection, f'{{"method":"setInputMode", "params": {{"aid": "{aid}", "mode": "resize" }} }}')


#send_msg(connection, f'{{"method":"setTheme","params":{{"aid": "{aid}", "statusBarColor": {0xff003180},"colorPrimary": {0xffd1d1d1},"windowBackground": {0xffdedede},"textColor": {0xff000000},"colorAccent": {0xff003180} }} }}')


send_msg(connection, f'{{"method":"createNestedScrollView", "params": {{"aid": "{aid}"}} }}')
sv = read_msg(connection)



send_msg(connection, f'{{"method":"createLinearLayout", "params": {{"aid": "{aid}"}}, "parent": {sv} }}')
root = read_msg(connection)


send_msg(connection, f'{{"method":"createButton", "params": {{"aid": "{aid}", "parent": {root}, "text": "Save" }} }}')
bt = read_msg(connection)

send_msg(connection, f'{{"method":"createEditText", "params": {{"aid": "{aid}", "parent": {root} }} }}')
et = read_msg(connection)

send_msg(connection, f'{{"method":"setLinearLayoutParams", "params": {{"aid": "{aid}", "id": {et}, "weight": 10 }} }}')





f = io.open("lockscreennotes.txt", "a", encoding="utf-8")


while True:
    ev = read_msg(connection2)
    if ev["type"] == "destroy" and ev["value"]["finishing"] == True:
        sys.exit()
    if ev["type"] == "click" and ev["value"]["id"] == bt:
        send_msg(connection, f'{{"method":"getText", "params": {{"aid": "{aid}", "id": {et} }} }}')
        f.write(read_msg(connection))
        f.write("\n")
        send_msg(connection, f'{{"method":"setText", "params": {{"aid": "{aid}", "id": {et}, "text": "" }} }}')
    
    
    
    
