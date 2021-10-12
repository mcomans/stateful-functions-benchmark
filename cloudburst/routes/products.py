from flask import Blueprint, request
from uuid import uuid4

products = Blueprint('products', __name__)

@products.post("/")
def create_product():
  product_id = str(uuid4())

  return product_id