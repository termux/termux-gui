
def __read_msg(s, msg):
    b = b''
    togo = 4
    while togo > 0:
        read = s.recv(togo)
        b = b + read
        togo = togo - len(read)
    togo = int.from_bytes(b, "big")
    b = b''
    while togo > 0:
        read = s.recv(togo)
        b = b + read
        togo = togo - len(read)
    return json.loads(b.decode("utf-8"))

