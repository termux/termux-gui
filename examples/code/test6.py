import socket
import sys
import os
import struct
import time
import json
import base64
import mmap
import array


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


send_msg(connection, '{"method":"newActivity", "params": {"dialog": true} }')
aid, tid = (read_msg(connection))



send_msg(connection, f'{{"method":"setTaskDescription","params":{{"aid":"{aid}","label":"Input Dialog" }} }}')

#send_msg(connection, f'{{"method":"setTheme","params":{{"aid": "{aid}", "statusBarColor": {0xff003180},"colorPrimary": {0xffd1d1d1},"windowBackground": {0xffdedede},"textColor": {0xff000000},"colorAccent": {0xff003180} }} }}')

send_msg(connection, f'{{"method":"createLinearLayout", "params": {{"aid": "{aid}"}} }}')
root = read_msg(connection)


send_msg(connection, f'{{"method":"createTextView", "params": {{"aid": "{aid}", "parent": {root}, "text": "Input Dialog" }} }}')
tv = read_msg(connection)

send_msg(connection, f'{{"method":"setTextSize", "params": {{"aid": "{aid}", "id": {tv}, "size": 30 }} }}')
send_msg(connection, f'{{"method":"setMargin", "params": {{"aid": "{aid}", "id": {tv}, "margin": 10 }} }}')



send_msg(connection, f'{{"method":"createTextView", "params": {{"aid": "{aid}", "parent": {root}, "text": "In difference to Termux:API, you can create dialogs with custom Layouts." }} }}')
tv2 = read_msg(connection)
send_msg(connection, f'{{"method":"setMargin", "params": {{"aid": "{aid}", "id": {tv2}, "margin": 5 }} }}')

send_msg(connection, f'{{"method":"createEditText", "params": {{"aid": "{aid}", "parent": {root} }} }}')
et = read_msg(connection)

send_msg(connection, f'{{"method":"setMargin", "params": {{"aid": "{aid}", "id": {et}, "margin": 5 }} }}')

send_msg(connection, f'{{"method":"createCheckbox", "params": {{"aid": "{aid}", "parent": {root}, "text": "show next TextView" }} }}')
check = read_msg(connection)

send_msg(connection, f'{{"method":"setMargin", "params": {{"aid": "{aid}", "id": {check}, "margin": 5 }} }}')

tv3 = None

send_msg(connection, f'{{"method":"createButton", "params": {{"aid": "{aid}", "parent": {root}, "text": "submit" }} }}')
bt = read_msg(connection)

send_msg(connection, f'{{"method":"setMargin", "params": {{"aid": "{aid}", "id": {bt}, "margin": 5 }} }}')

while True:
    ev = read_msg(connection2)
    if ev["type"] == "stop" and ev["value"]["finishing"] == True:
        sys.exit()
    if ev["type"] == "click" and ev["value"]["id"] == bt:
        send_msg(connection, f'{{"method":"getText", "params": {{"aid": "{aid}", "id": {et} }} }}')
        print(read_msg(connection))
        
        sys.exit()
    if ev["type"] == "click" and ev["value"]["id"] == check:
        checked = ev["value"]["set"]
        if checked and tv3 == None:
            send_msg(connection, f'{{"method":"createTextView", "params": {{"aid": "{aid}", "parent": {root}, "text": "And even change the Layout while the Dialog is showing." }} }}')
            tv3 = read_msg(connection)
            send_msg(connection, f'{{"method":"setMargin", "params": {{"aid": "{aid}", "id": {tv3}, "margin": 5 }} }}')
        if not checked and tv3 != None:
            send_msg(connection, f'{{"method":"deleteView", "params": {{"aid": "{aid}", "id": {tv3} }} }}')
            tv3 = None
            
            
