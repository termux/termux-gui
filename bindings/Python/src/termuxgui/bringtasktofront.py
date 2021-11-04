from json import dumps

from termuxgui import __send_msg

def bringtasktofront(tid):
    '''Bring an existing Task to the front and shows it.'''
    __send_msg(dumps({"method": "bringTaskToFront", "params": {"tid": tid}}))
