from json import dumps

from termuxgui import __send_msg

def settaskdescription(aid, text, img):
    '''Sets the Task icon. img has to be a PNG or JPEG image as a base64 encoded string.'''
    __send_msg(dumps({"method": "setTaskDescription", "params": {"aid": aid, "img": img, "label": text}}))
