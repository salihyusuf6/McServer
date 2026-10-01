from flask import Flask, request
import db
import config
app = Flask(__name__)

@app.route("/")
def home():
    return "Merhaba Flask!"

@app.post("/")
def temp():
    data = request.get_data()
    print(data)
    return "HAPPY"

@app.post("/NameCheck")
def NameCheck():
    data = request.get_json()#returns dict :)
    try:
        secretKey = data["Secret"]
        if secretKey != config.PlayerValidationCheck:
            return 403
        return str(db.CheckPlayer(data["Name"]))
    except Exception as e:
        print(e)
        return 404
@app.post("/Login")
def Login():
    data = request.get_json()#returns dict :)
    try:
        secretKey = data["Secret"]
        if secretKey != config.PlayerLoginCheck:
            return 403
        return str(db.LoginEvent(data["Name"],data["Password"]))
    except Exception as e:
        print(e)
        return 404


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)