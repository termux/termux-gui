from json import dumps

from termuxgui import __send_msg

def settheme(aid, statusbarcolor, colorprimary, windowbackground, textcolor, coloraccent):
    '''Sets the Theme fro an Activity.'''
    __send_msg(dumps({"method": "setTheme", "params": {"aid": aid, "statusBarColor": statusbarcolor, "colorPrimary": colorprimary, "windowBackground": windowbackground, "textColor": textcolor, "colorAccent": coloraccent}}))
