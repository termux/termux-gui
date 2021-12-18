import time
import sys

import termuxgui as tg


with tg.Connection() as c:
    
    a = tg.Activity(c)
    
    topl = tg.LinearLayout(a)
    
    tabs = tg.TabLayout(a, topl)
    
    tabs.setlist(["Tab 1", "Tab 2"])
    
    tabs.setheight(tg.WRAP_CONTENT)
    tabs.setlinearlayoutparams(0)
    
    s = tg.HorizontalScrollView(a, topl, fillviewport=True, snapping=True, nobar=True)
    
    l = tg.LinearLayout(a, s, vertical=False)
    while s.getdimensions()[0] == 0:
        time.sleep(0.001)
    w = s.getdimensions()[0]
    
    b1 = tg.Button(a, "Button 1", l)
    b1.setwidth(w, True)
    b1.setlinearlayoutparams(0)
    b2 = tg.Button(a, "Button 2", l)
    b2.setwidth(w, True)
    b2.setlinearlayoutparams(0)
    
    
    
    
    for ev in c.events():
        if ev.type == "destroy":
            sys.exit()
        if ev.type == "itemselected" and ev.id == tabs:
            print("set scroll to:",w*ev.value["selected"])
            s.setscrollposition(w*ev.value["selected"], 0)
        print(ev.value)
