import time
import sys

import termuxgui as tg


with tg.Connection() as c:
    
    a = tg.Activity(c)
    
    s = tg.NestedScrollView(a, fillviewport=True, snapping=True, nobar=True)
    
    l = tg.LinearLayout(a, s)
    while s.getdimensions()[0] == 0:
        time.sleep(0.001)
    h = s.getdimensions()[1]
    
    b1 = tg.Button(a, "Button 1", l)
    b1.setheight(h, True)
    b1.setlinearlayoutparams(0)
    b2 = tg.Button(a, "Button 2", l)
    b2.setheight(h, True)
    b2.setlinearlayoutparams(0)
    
    time.sleep(0.1)
    
    s.setscrollposition(0, h, False)
    
    
    time.sleep(3)
    print(s.getscrollposition())
    
    
    for ev in c.events():
        if ev.type == "destroy":
            sys.exit()
