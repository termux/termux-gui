import socket
import sys
import os
import struct
import time
import json
import base64
from subprocess import Popen, PIPE

icon = ""
with open("icon.png","rb") as f:
    icon = base64.standard_b64encode(f.read()).decode("ascii")

img = ""
with open(sys.argv[1],"rb") as f:
    img = base64.standard_b64encode(f.read()).decode("ascii")

w=0
h=0
process = Popen(f"identify -format '%wx%h' '{sys.argv[1]}'", stdout=PIPE, shell=True)
(output, err) = process.communicate()
exit_code = process.wait()
output = output.decode("ascii")
if exit_code == 0:
    w = output.split("x")[0]
    h = output.split("x")[1]


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


send_msg(connection, '{"method":"newActivity", "params": {"pip": true}}')
aid, tid = (read_msg(connection))

if w != 0:
    send_msg(connection, f'{{"method":"setPiPParams","params":{{"aid":"{aid}","num": {w}, "den": {h} }} }}')

send_msg(connection, f'{{"method":"setTheme","params":{{"aid": "{aid}", "statusBarColor": {0xff003180},"colorPrimary": {0xffd1d1d1},"windowBackground": {0xffdedede},"textColor": {0xff000000},"colorAccent": {0xff003180} }} }}')

send_msg(connection, f'{{"method":"setTaskDescription","params":{{"aid":"{aid}","img":"{icon}" }} }}')


send_msg(connection, f'{{"method":"createImageView","params":{{"aid": "{aid}"}} }}')
id = read_msg(connection)

#print("id:",id)

send_msg(connection, f'{{"method":"setImage","params":{{"aid": "{aid}", "id": "{id}","img":"{img}" }} }}')

time.sleep(5)

connection.close()
connection2.close()



os.system("am start --user 0 -n com.termux/.app.TermuxActivity >/dev/null 2>&1")
