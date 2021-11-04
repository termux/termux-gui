from json import dumps

from termuxgui import __send_msg

def finishtask(tid):
    '''Finishes a Task and removes it from the recent tasks screen.'''
    __send_msg(dumps({"method": "finishTask", "params": {"tid": tid}}))
