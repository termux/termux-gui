import time
import sys

import termuxgui as tg


with tg.Connection() as c:
    
    a = tg.Activity(c, dialog=True)
    
    ref = tg.SwipeRefreshLayout(a)
    
    l = tg.LinearLayout(a, ref)
    
    t = tg.EditText(a, "", l)
    t.setmargin(10)
    
    t.focus(True)
    time.sleep(2)
    a.hidesoftkeyboard()
    
    
    for ev in c.events():
        print(ev.type, ev.value)
        if ev.type == tg.Event.refresh:
            ref.setrefreshing(False)
            t.setvisibility(2)
        if ev.type == "destroy":
            sys.exit()
