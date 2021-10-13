def register_carts_functions(cloud):
  def add_product_to_cart(cb, cart_id, product_id, amount):
    cart = cb.get(cart_id)

    if cart is None:
      cart = {}

    if product_id in cart:
      cart[product_id] += amount
    else:
      cart[product_id] = amount
    
    cb.put(cart_id, cart)
    return cart

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

  def get_cart_contents(cb, cart_id):
    cart = cb.get(cart_id)

    return cart
  
  cloud.register(add_product_to_cart, "add_product_to_cart")
  cloud.register(remove_product_from_cart, "remove_product_from_cart")
  cloud.register(get_cart_contents, "get_cart_contents")