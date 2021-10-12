from flask import Blueprint, request
from uuid import uuid4
from cloud import cloud

carts = Blueprint('shopping-carts', __name__)

add_to_cart = cloud.get_function("add_product_to_cart")

@carts.post("/")
def create_shopping_cart():
  cart_id = str(uuid4())

  return cart_id

@carts.route("/<cart_id>", methods=["POST"])
def add_to_cart(cart_id):
  data = request.json
  res = add_to_cart(cart_id, data["productId"], data["amount"]).get()
  return res