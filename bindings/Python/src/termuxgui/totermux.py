from subprocess import run, DEVNULL

def totermux():
    '''Returns to the termux task. This is a shorthand for running am start to start the termux activity.'''
    run(["am","start","--user", "0","-n","com.termux/.app.TermuxActivity"],stdout=DEVNULL,stderr=DEVNULL)
