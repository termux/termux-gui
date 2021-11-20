import time
import sys
import threading
from ctypes import *

from sdl2 import *
import sdl2.ext

import termuxgui as tg


resolution = 1000

c = tg.Connection()

b = tg.Buffer(c, resolution, resolution)

conlock = threading.Lock()

a = tg.Activity(c)

layout = tg.LinearLayout(a)

im = tg.ImageView(a, layout)



im.setbuffer(b)
im.sendtouchevent(True)

interface = tg.LinearLayout(a, layout)

massedit = tg.EditText(a, "10", interface, singleline=True)
massedit.setheight("WRAP_CONTENT")

sim = tg.ToggleButton(a, interface)
sim.setchecked(True)







memp = cast(pointer(c_uint8.from_buffer(b.mem, 0)), c_void_p)

SDL_Init(SDL_INIT_VIDEO)

surf = SDL_CreateRGBSurfaceFrom(memp, resolution, resolution, 32, 4*resolution, c_uint(0xff), c_uint(0xff00), c_uint(0xff0000), c_uint(0xff000000))


def drawcircle(surf, x, y ,r, color):
    # too lazy to actually write a circle drawing algorithm that performs good enough in python
    sdl2.ext.fill(surf, color, (x-r/2, y-r/2, r, r))
    return
    view = sdl2.ext.PixelView(surf)
    for xp in range(-r, r):
        for yp in range(-r, r):
            if xp*xp + yp*yp <= r*r:
                try:
                    view[y+yp][x+xp] = color
                except IndexError:
                    pass
    del view
        

def vectlen(vect):
    return (vect[0] ** 2 + vect[1] ** 2) ** 0.5

def vectinv(vect):
    return (-vect[0], -vect[1])

def norm(vect):
    len = vectlen(vect)
    return (vect[0] / len, vect[1] / len)

def dist(a,b):
    return ((a[0] + b[0]) ** 2 + (a[1] + b[1]) ** 2) ** 0.5


class Mass:
    #G = 6.67430 * (10 ** -11)
    G = 1
    def __init__(self, x, y, mass):
        self.x = x
        self.y = y
        self.dx = 0
        self.dy = 0
        self.m = mass
    
    
    
    def attract(self, other):
        r = dist((self.x, self.y), (other.x, other.y))
        f = Mass.G * ((self.m * other.m) / r)
        aself = f / self.m
        aother = f / other.m
        vect = norm((other.x - self.x, other.y - self.y))
        vectother = vectinv(vect)
        self.dx += vect[0]*aself
        self.dy += vect[1]*aself
        other.dx += vectother[0]*aother
        other.dy += vectother[1]*aother
    

m1 = Mass(250, 100, 40)
m1.dx = 3

m2 = Mass(250, 400, 40)
m2.dx = -3

world = [m1, m2]

blue = blue = sdl2.ext.Color(0, 0, 255, 255)

stopped = False

def render():
    while True:
        #print("loop")
        t1 = time.time()
        
        white = sdl2.ext.Color(255, 255, 255, 255)
        sdl2.ext.fill(surf, white)
        
        #print("screen cleared")
        for m in world:
            drawcircle(surf, int(m.x), int(m.y), int(m.m), blue)
        
        if not stopped:
            for m1 in world:
                for m2 in world:
                    if m1 != m2:
                        m1.attract(m2)
            for m in world:
                m.x += m.dx
                m.y += m.dy
        
        
        with conlock:
            b.blit()
            im.refresh()
        t2 = time.time()
        #print("drawing finished")
        time.sleep(max(0, 0.030-abs(t2-t1))) # target 30 fps
    
renderer = threading.Thread(target=render, daemon=True)
renderer.start()

x = 0
y = 0
multi = False

for ev in c.events():
    if ev.type == tg.Event.destroy and ev.value["finishing"]:
        sys.exit()
    if ev.type == tg.Event.click and ev.id == sim:
        stopped = not ev.value["set"]
    if ev.type == tg.Event.touch and ev.id == im:
        if ev.value["action"] == "down":
            p1 = ev.value["pointers"][0]
            x = p1["x"]
            y = p1["y"]
        if ev.value["action"] == "pointer_down":
            multi = True
        if ev.value["action"] == "up":
            if multi:
                multi = False
                continue
            removed = False
            for m in world:
                if m.x-m.m/2 <= x and x < m.x+m.m/2 and m.y-m.m/2 <= y and y < m.y+m.m/2:
                    removed = True
                    world.remove(m)
                    break
            if removed:
                continue
            p1 = ev.value["pointers"][0]
            dx = x-p1["x"]
            dy = y-p1["y"]
            with conlock:
                m = Mass(p1["x"], p1["y"], int(massedit.gettext()))
            m.dx = dx/10
            m.dy = dy/10
            world.append(m)
    
    
    
