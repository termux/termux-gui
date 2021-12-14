import time
import sys

import termuxgui as tg


with tg.Connection() as c:
    
    a = tg.Activity(c)
    
    h = tg.NestedScrollView(a, fillviewport=True, snapping=True, nobar=True)
    
    l = tg.LinearLayout(a, h)
    while h.getdimensions()[0] == 0:
        time.sleep(0.001)
    h = h.getdimensions()[1]
    
    b1 = tg.Button(a, "Button 1", l)
    b1.setheight(h, True)
    b1.setlinearlayoutparams(0)
    b2 = tg.Button(a, "Button 2", l)
    b2.setheight(h, True)
    b2.setlinearlayoutparams(0)
    
    
    for ev in c.events():
        if ev.type == "destroy":
            sys.exit()
