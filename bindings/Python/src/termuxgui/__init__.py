__all__ = ["activity", "bringtasktofront", "connect", "finishactivity", 
"finishtask", "setpipparams", "settaskicon", "settheme", "totermux"]

for m in __all__:
    exec("from termuxgui."+m+" import "+m)
del m

