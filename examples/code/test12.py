import time
import sys

import termuxgui as tg


with tg.Connection() as c:
    
    a = tg.Activity(c)
    
    l = tg.LinearLayout(a)
    
    t = tg.EditText(a, "Title", l, blockinput=True)
    
    
    
    
    for ev in c.events():
        print(ev.type, ev.value)
        if ev.type == "destroy":
            sys.exit()
