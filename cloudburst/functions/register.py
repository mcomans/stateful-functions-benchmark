from functions.products import register_products_functions
from functions.users import register_users_functions

def register_functions(cloud):
  register_products_functions(cloud)
  register_users_functions(cloud)

  def add_to_cart(cb, cart_id, product_id, amount):
    cart = cb.get(cart_id)

    if cart is None:
      cart = {}

    if product_id in cart:
      cart[product_id] += amount
    else:
      cart[product_id] = amount
    
    cb.put(cart_id, cart)
    return str(cart)

  cloud.register(add_to_cart, "add_to_cart")