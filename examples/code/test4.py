import socket
import sys
import os
import struct
import time
import json
import base64
import mmap
import array

icon = ""
with open("icon.png","rb") as f:
    icon = base64.standard_b64encode(f.read()).decode("ascii")


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

def read_msg_fd(s):
    b = b''
    togo = 4
    while togo > 0:
        read = s.recv(togo)
        b = b + read
        togo = togo - len(read)
    togo = int.from_bytes(b, "big")
    b = b''
    fds = array.array("i")
    while togo > 0:
        read, ancdata, _, _ = s.recvmsg(togo, socket.CMSG_LEN(fds.itemsize))
        for cmsg_level, cmsg_type, cmsg_data in ancdata:
            if cmsg_level == socket.SOL_SOCKET and cmsg_type == socket.SCM_RIGHTS:
                fds.frombytes(cmsg_data[:len(cmsg_data) - (len(cmsg_data) % fds.itemsize)])
        b = b + read
        togo = togo - len(read)
    if len(fds) != 0:
        return [json.loads(b.decode("utf-8")), fds[0]]
    else:
        return [json.loads(b.decode("utf-8"))]


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


send_msg(connection, '{"method":"newActivity"}')
aid, tid = (read_msg(connection))



send_msg(connection, f'{{"method":"setTheme","params":{{"aid": "{aid}", "statusBarColor": {0xff003180},"colorPrimary": {0xffd1d1d1},"windowBackground": {0xffdedede},"textColor": {0xff000000},"colorAccent": {0xff003180} }} }}')

send_msg(connection, f'{{"method":"setTaskDescription","params":{{"aid":"{aid}","img":"{icon}" }} }}')


send_msg(connection, f'{{"method":"createImageView","params":{{"aid": "{aid}"}} }}')
id = read_msg(connection)


send_msg(connection, f'{{"method":"addBuffer","params":{{"format": "ARGB888", w: 100, h: 100}} }}')
ret = read_msg_fd(connection)
bid=0
fd=0
if len(ret) == 1:
    sys.exit()
else:
    bid = ret[0]
    fd = ret[1]



mem = mmap.mmap(fd, 100*100*4)



for i in range(0,100*100):
    mem.write(b'\xff\x00\x00\xff')
mem.seek(0)
mem.flush()

send_msg(connection, f'{{"method":"blitBuffer","params":{{"bid": {bid}}} }}')
send_msg(connection, f'{{"method":"setBuffer","params":{{"aid": "{aid}", "id": "{id}","bid": {bid} }} }}')

time.sleep(2)


for i in range(0,100*100):
    mem.write(b'\x00\xff\x00\xff')
mem.seek(0)
mem.flush()

send_msg(connection, f'{{"method":"blitBuffer","params":{{"bid": {bid}}} }}')
send_msg(connection, f'{{"method":"refreshImageView","params":{{"aid": "{aid}", "id": "{id}" }} }}')

time.sleep(2)

connection.close()
connection2.close()

mem.close()

os.system("am start --user 0 -n com.termux/.app.TermuxActivity >/dev/null 2>&1")
