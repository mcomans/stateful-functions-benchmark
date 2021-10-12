from flask import Blueprint, request
from uuid import uuid4

orders = Blueprint('orders', __name__)

@orders.post("/")
def create_order():
  order_id = str(uuid4())

  return order_id