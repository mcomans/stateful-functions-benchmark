from cloudburst.client.client import CloudburstConnection
from flask import Flask, request
from benchmark import register

local_cloud = CloudburstConnection('127.0.0.1', '127.0.0.1', local=True)

app = Flask(__name__)

register.register_functions(local_cloud)

add_to_cart = local_cloud.get_function("add_to_cart")


@app.route("/", methods=["POST"])
def test():

  data = request.json
  res = add_to_cart("cart1", data["product_id"], data["amount"]).get()
  print(res)
  return res