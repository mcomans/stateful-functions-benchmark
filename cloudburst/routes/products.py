from flask import Blueprint, request
from uuid import uuid4
from cloud import cloud

products = Blueprint('products', __name__)

set_product_price = cloud.get_function("set_product_price")
add_product_stock = cloud.get_function("add_product_stock")

@products.post("/")
def create_product():
  product_id = str(uuid4())
  product_body = request.json

  if product_body is not None and "price" in product_body:
    product = set_product_price(product_id, product_body["price"]).get()
  if product_body is not None and "stock" in product_body:
    product = add_product_stock(product_id, product_body["stock"]).get()

  return product_id, 201

@products.patch("/<product_id>")
def patch_product(product_id):
  product_body = request.json

  if product_body is not None and "price" in product_body:
    product = set_product_price(product_id, product_body["price"]).get()
  if product_body is not None and "stock" in product_body:
    product = add_product_stock(product_id, product_body["stock"]).get()
  
  return product
