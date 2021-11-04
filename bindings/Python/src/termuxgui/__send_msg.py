
def __send_msg(c, msg):
    m = bytes(msg, "utf-8")
    connection.sendall((len(m)).to_bytes(4,"big"))
    connection.sendall(m)