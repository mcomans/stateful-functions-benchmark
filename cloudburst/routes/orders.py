from flask import Blueprint, request
from cloud import cloud

orders = Blueprint('orders', __name__)

get_cart_contents = cloud.get_function("get_cart_contents")
retract_product_stock = cloud.get_function("retract_product_stock")
retract_user_credits = cloud.get_function("retract_user_credits")

@orders.post("/checkout")
def checkout_order():
  def checkout_change_stock(cb, is_needed, product_id, amount):
    if not is_needed:
      return {'success': True, 'did_change': False}
    
    product = cb.get(product_id)

    if not product:
      return {'success': False, 'did_change': False}

    if product['stock'] + int(amount) < 0:
      return {'success': False, 'did_change': False}

    product['stock'] += int(amount)

    cb.put(product_id, product)

    return {'success': True, 'price': product['price'], 'did_change': True}

  def intermediary_1(cb, *results):
    if False in [result['success'] for result in results]:
      

  order_body = request.json

  cart_id = order_body["shoppingCartId"]
  cart_contents = get_cart_contents(cart_id).get()

  futures = [(retract_product_stock(product, amount), amount) for product, amount in cart_contents.items()]
  prices = [int(future[0].get()["price"]) * int(future[1]) for future in futures]
  print(prices)
  total = sum(prices)

  user_id = order_body["userId"]

  user = retract_user_credits(user_id, total).get()

  return {"cart": cart_contents, "total": total, "user": user}