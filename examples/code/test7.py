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
#from PIL import Image, ImageDraw, ImageFont
from ctypes import *
from sdl2 import *
import sdl2.ext

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

send_msg(connection, f'{{"method":"setTaskIcon","params":{{"aid":"{aid}","img":"{icon}" }} }}')


send_msg(connection, f'{{"method":"createImageView","params":{{"aid": "{aid}"}} }}')
id = read_msg(connection)


send_msg(connection, f'{{"method":"addBuffer","params":{{"format": "ARGB888", w: 500, h: 500}} }}')
ret = read_msg_fd(connection)
bid=0
fd=0
if len(ret) == 1:
    sys.exit()
else:
    bid = ret[0]
    fd = ret[1]



mem = mmap.mmap(fd, 500*500*4)
send_msg(connection, f'{{"method":"setBuffer","params":{{"aid": "{aid}", "id": "{id}","bid": {bid} }} }}')

memp = cast(pointer(c_uint8.from_buffer(mem, 0)), c_void_p)

SDL_init(SDL_INIT_VIDEO)

surf = SDL_CreateRGBSurfaceFrom(memp,c_int(500),  c_int(500), c_int(32), c_int(500*4), c_uint(0xff), c_uint(0xff00), c_uint(0xff0000), c_uint(0xff000000))
ren = SDL_CreateSoftwareRenderer(surf)

mf = sdl2.ext.FontManager("LiberationSans-Regular.ttf",size=20)

for i in range(0,1000):
    
    SDL_SetRenderDrawColor(ren, c_unit8(255), c_unit8(255), c_unit8(255), c_unit8(255))
    SDL_RenderClear()
    
    SDL_SetRenderDrawColor(ren, c_unit8(0), c_unit8(0), c_unit8(0), c_unit8(255))
    
    
    
    
    
    SDL_RenderPresent(ren)
    mem.flush()
    time.sleep(0.01)






'''
img = Image.new("RGBA", (500,500))

#img = Image.frombuffer("RGBA", (500,500), mem)

draw = ImageDraw.Draw(img)

font = ImageFont.truetype("LiberationSans-Regular.ttf", 30)

for i in range(0,10):
    
    
    
    draw.rectangle((0,0,500,500),outline=(255,255,255,255),fill=(255,255,255,255))
    
    
    draw.text((10,10), "Displaying dynamic content\nin a framebuffer", (0,0,0,255), font=font)
    
    
    
    mem.seek(0)
    
    for y in range(0, 500):
        for x in range(0,500):
            r, g, b, a = img.getpixel((x, y))
            mem.write(r.to_bytes(1, sys.byteorder))
            mem.write(g.to_bytes(1, sys.byteorder))
            mem.write(b.to_bytes(1, sys.byteorder))
            mem.write(a.to_bytes(1, sys.byteorder))
    
    
    
    mem.flush()
    send_msg(connection, f'{{"method":"blitBuffer","params":{{"bid": {bid}}} }}')
    send_msg(connection, f'{{"method":"refreshImageView","params":{{"aid": "{aid}", "id": "{id}" }} }}')
    time.sleep(0.1)
'''







connection.close()
connection2.close()

mem.close()

os.system("am start --user 0 -n com.termux/.app.TermuxActivity >/dev/null 2>&1")
