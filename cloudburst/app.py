from cloudburst.client.client import CloudburstConnection
from flask import Flask, request
from benchmark import register

local_cloud = CloudburstConnection('127.0.0.1', '127.0.0.1', local=True)

app = Flask(__name__)

register.register_functions(local_cloud)

# atc_lambda = local_cloud.get_function("add_to_cart_lambda")
# atc_func = local_cloud.get_function("add_to_cart_func")
# atc_class = local_cloud.get_function("add_to_cart_class")

add_to_cart = local_cloud.get_function("add_to_cart")


@app.route("/", methods=["POST"])
def test():
  # res = atc_lambda().get()
  # print("Lambda: " + str(res))
  # res = atc_func("test_id2").get()
  # print("Function: " + str(res))
  # res = atc_class("test_class_func").get()
  # print("Class: " + str(res))
  # return "test"
  data = request.json
  res = add_to_cart("cart1", data["product_id"], data["amount"]).get()
  print(res)
  return res