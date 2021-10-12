def add_product_to_cart(cb, cart_id, product_id, amount):
  cart = cb.get(cart_id)

  if cart is None:
    cart = {}

  if product_id in cart:
    cart[product_id] += amount
  else:
    cart[product_id] = amount
  
  cb.put(cart_id, cart)
  return str(cart)

def remove_product_from_cart(cb, cart_id, product_id, amount):
  cart = cb.get(cart_id)

  if cart is None:
    return None

  if product_id in cart:
    cart[product_id] = max(0, cart[product_id] - amount)
  else:
    cart[product_id] = amount
  
  cb.put(cart_id, cart)
  return str(cart)

def register_carts_functions(cloud):
  cloud.register(add_product_to_cart, "add_product_to_cloud")
  cloud.register(remove_product_from_cart, "remove_product_from_cloud")