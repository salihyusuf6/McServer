#Make a Proper DB BRO
users = {
    "OmenTonremmm":"hello"
}


def CheckPlayer(name:str)->bool:
    return True if users[name] != None else False
def LoginEvent(name:str,password:str)->bool:
    if not(users[name] != None):
        return False
    if password == users[name]:
        return True
    else:
        return False
