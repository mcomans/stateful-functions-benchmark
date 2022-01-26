from flask import Blueprint, request
from cloud import cloud
from uuid import uuid4

orders = Blueprint('orders', __name__)

get_cart_contents = cloud.get_function("get_cart_contents")
retract_product_stock = cloud.get_function("retract_product_stock")
retract_user_credits = cloud.get_function("retract_user_credits")

@orders.post("/checkout")
def checkout_order():
  request_id = str(uuid4())
  checkout = request.json
  if checkout:
    cloud.call_dag("checkout", {
        "checkout_get_cart_contents": {"user_id": checkout["userId"], "cart_id": checkout["cartId"], "request_id": request_id}
    })
    return "success?"
  else:
    return "no checkout body", 400
