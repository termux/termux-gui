import time
import sys

import termuxgui as tg


with tg.Connection() as c:
    
    a = tg.Activity(c)
    
    l = tg.GridLayout(a, 2, 2)
    
    l11 = tg.TextView(a, "11", l)
    l11.setgridlayoutparams(0, 0)
    l12 = tg.TextView(a, "12", l)
    l12.setgridlayoutparams(0, 1)
    l21 = tg.TextView(a, "21", l)
    l21.setgridlayoutparams(1, 0)
    l22 = tg.TextView(a, "22", l)
    l22.setgridlayoutparams(1, 1)
    
    
    
    
    
    #t = tg.EditText(a, "Title", l, blockinput=True)
    
    
    
    
    for ev in c.events():
        print(ev.type, ev.value)
        if ev.type == "destroy":
            sys.exit()
