from json import dumps

from termuxgui import __send_msg

def finishactivity(aid):
    '''Finishes an Activity. For the first Activity in a Task, use finishTask instead.'''
    __send_msg(dumps({"method": "finishActivity", "params": {"aid": aid}}))
