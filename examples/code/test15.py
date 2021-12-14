import time
import sys

import termuxgui as tg


with tg.Connection() as c:
    
    a = tg.Activity(c)
    
    ref = tg.SwipeRefreshLayout(a)
    
    l = tg.LinearLayout(a, ref)
    
    t = tg.TextView(a, "Title", l)
    t.settextsize(40)
    t.setbackgroundcolor(0xffffffff)
    t.settextcolor(0xff0000ff)
    
    t.setlinearlayoutparams(0)
    t.setdimensions(100,100, True)
    
    t.setvisibility(0)
    
    
    
    t.sendclickevent(True)
    t.sendlongclickevent(True)
    
    
    
    
    
    pb = tg.ProgressBar(a, l)
    pb.setprogress(25)
    pb.setbackgroundcolor(0xffffffff)
    
    print(t.getdimensions())
    
    
    
    
    
    
    for ev in c.events():
        print(ev.type, ev.value)
        if ev.type == tg.Event.refresh:
            ref.setrefreshing(False)
            t.setvisibility(2)
        if ev.type == "destroy":
            sys.exit()
