from flask import Blueprint, request
from cloud import cloud

orders = Blueprint('orders', __name__)

get_cart_contents = cloud.get_function("get_cart_contents")
retract_product_stock = cloud.get_function("retract_product_stock")
retract_user_credits = cloud.get_function("retract_user_credits")

@orders.post("/checkout")
def checkout_order():
  order_body = request.json

  cart_id = order_body["cartId"]
  cart_contents = get_cart_contents(cart_id).get()

  futures = [(retract_product_stock(product, amount), amount) for product, amount in cart_contents.items()]
  prices = [int(future[0].get()["price"]) * int(future[1]) for future in futures]
  print(prices)
  total = sum(prices)

  user_id = order_body["userId"]

  user = retract_user_credits(user_id, total).get()

  return {"cart": cart_contents, "total": total, "user": user}