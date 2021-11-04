from json import dumps

from termuxgui import __send_msg

def setpipparams(aid, num, den):
    '''Sets the PiP parameters for the Activity, the aspect ration.'''
    __send_msg(dumps({"method": "setPiPParams", "params": {"aid": aid, "num": num, "den": den}}))
