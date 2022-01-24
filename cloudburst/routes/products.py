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
  
  return product_id

@products.get("/<product_id>/freq-items")
def get_freq_items(product_id):
  request_id = uuid4()
  depth_str = request.args.get("depth")
  depth = int(depth_str) if depth_str else 3

  cloud.register_dag(str(request_id),
                     ["get_top_freq_item_" + str(i) for i in range(1, depth+1)],
                     [("get_top_freq_item_" + str(i), "get_top_freq_item_" + str(i + 1)) for i in range(1, depth)]
                     )

  cloud.call_dag(str(request_id), {
    "get_top_freq_item_1": {"found_items": [], "product_id": product_id, "request_id": request_id}
  }).get()

  cloud.delete_dag(str(request_id))

  cloud
