def register_functions(cloud):
  # class AddToCartFunc:
  #   def __init__(self, cb):
  #    return None

  #   def run(self, cb, arg):
  #     return arg + "test"

  # atc_class = cloud.register((AddToCartFunc, ()), 'add_to_cart_class')
  # add_to_cart = lambda cb: cb.getid()
  # def add_to_cart_2(_, cart_id): return cart_id + "test"

  # cloud_atc = cloud.register(add_to_cart, 'add_to_cart_lambda')
  # cloud_atc2 = cloud.register(add_to_cart_2, 'add_to_cart_func')

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