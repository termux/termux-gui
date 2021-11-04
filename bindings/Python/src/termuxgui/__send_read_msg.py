from termuxgui import __read_msg
from termuxgui import __send_msg

def __send_read_msg(s, msg):
    send_msg(s, msg)
    return read_msg(s)
