from json import dumps

from termuxgui import __send_read_msg

def activity(tid=None,flags=0,dialog=None,pip=False):
    '''Creates and Activity and returns the Activity and Task id.'''
    return __send_read_msg(dumps({"method": "newActivity", "params": {"tid": tid, "flags": flags, "dialog": dialog, "pip": pip}}))
