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

row1 = tg.LinearLayout(a, interface, vertical=False)
row2 = tg.LinearLayout(a, interface, vertical=False)
row3 = tg.LinearLayout(a, interface, vertical=False)

masstext = tg.TextView(a, "Mass: ", row1)
masstext.setheight("WRAP_CONTENT")

massedit = tg.EditText(a, "100", row1, singleline=True)
massedit.setheight("WRAP_CONTENT")

clear = tg.Button(a, "Clear", row2)
clear.setheight("WRAP_CONTENT")



simtext = tg.TextView(a, "Simulation: ", row3)
simtext.setheight("WRAP_CONTENT")
sim = tg.ToggleButton(a, row3)
sim.setheight("WRAP_CONTENT")
sim.setchecked(True)


ox = 0
oy = 0
scale = 1.0



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
    try:
        return (vect[0] / len, vect[1] / len)
    except ZeroDivisionError:
        return (0, 0)

def dist(a,b):
    return ((a[0] + b[0]) ** 2 + (a[1] + b[1]) ** 2) ** 0.5


class Mass:
    #G = 6.67430 * (10 ** -11)
    G = 0.1
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
    
    def collision(self, other):
        sw = self.m ** 0.5
        ow = other.m ** 0.5
        if self.x < other.x+ow and self.x+sw > other.x and self.y < other.y+ow and self.y+sw > other.y:
            world.remove(other)
            vect = norm((self.dx, self.dy))
            vecto = norm((other.dx, other.dy))
            #print("collision",self.m, other.m, vect, vecto)
            mm = self.m + other.m
            self.dx = (vect[0]*self.m + vecto[0]*other.m) / mm
            self.dy = (vect[1]*self.m + vecto[1]*other.m) / mm
            #print(self.m, self.dx, self.dy)
            self.m = mm
            return True
        else:
            return False
        
        
    
    

m1 = Mass(250, 100, 400)
m1.dx = 3

m2 = Mass(250, 400, 400)
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
            drawcircle(surf, int(m.x+ox), int(m.y+oy), int(m.m ** 0.5), blue)
        
        if not stopped:
            for m1 in world:
                for m2 in world:
                    if m1 != m2:
                        if not m1.collision(m2):
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


p1 = None
x = 0
y = 0
multi = False
longp = False
moved = False
ptime = 0
lastdist = 0

for ev in c.events():
    if ev.type == tg.Event.destroy and ev.value["finishing"]:
        sys.exit()
    if ev.type == tg.Event.click and ev.id == clear:
        world = []
    if ev.type == tg.Event.click and ev.id == sim:
        stopped = not ev.value["set"]
    if ev.type == tg.Event.touch and ev.id == im:
        if ev.value["action"] == "down":
            #print("down")
            p1 = ev.value["pointers"][0]
            x = p1["x"]
            y = p1["y"]
            ptime = time.time()
        if ev.value["action"] == "pointer_down":
            #print("pointer down")
            multi = True
        if ev.value["action"] == "move":
            #print("move")
            if time.time()-ptime > 0.5 and not moved:
                #print("long")
                longp = True
            if not longp:
                for p in ev.value["pointers"]:
                    if p["id"] == p1["id"]:
                        #print(x, p["x"], y, p["y"])
                        ox -= (x-p["x"])
                        oy -= (y-p["y"])
                        x = p["x"]
                        y = p["y"]
                        #print(p1)
                        break;
            if len(ev.value["pointers"]) == 2:
                d = vectlen((abs(ev.value["pointers"][0]["x"]-ev.value["pointers"][1]["x"]), abs(ev.value["pointers"][0]["y"]-ev.value["pointers"][1]["y"])))
                if lastdist == 0:
                    lastdist = d
                else:
                    #print(lastdist-d)
                    lastdist = d
                
                
            moved = True
            
            
        if ev.value["action"] == "up":
            if time.time()-ptime > 0.5 and not moved:
                longp = True
            #print("up")
            lastdist = 0
            if multi or not longp:
                multi = False
                longp = False
                moved = False
                continue
            removed = False
            multi = False
            longp = False
            moved = False
            for m in world:
                w = m.m ** 0.5
                if m.x-w/2 <= x-ox and x-ox < m.x+w/2 and m.y-w/2 <= y-oy and y-oy < m.y+w/2:
                    removed = True
                    world.remove(m)
                    break
            if removed:
                continue
            p1 = ev.value["pointers"][0]
            dx = x-p1["x"]
            dy = y-p1["y"]
            with conlock:
                m = Mass(x-ox, y-oy, int(massedit.gettext()))
            m.dx = dx/10
            m.dy = dy/10
            world.append(m)
    
    
    
