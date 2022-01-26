from .logging import setup_logging
import structlog

setup_logging()
logger = structlog.get_logger()

def register_carts_functions(cloud):
  def add_product_to_cart(cb, cart_id, product_id, amount, request_id):
    log = logger.bind(function="add_product_to_cart", request_id=request_id, entity_id=cart_id)
    log.info("INCOMING")
    cart = cb.get(cart_id)

    if cart is None:
      cart = {}

    if product_id in cart:
      cart[product_id] += amount
    else:
      cart[product_id] = amount
    
    cb.put(cart_id, cart)
    log.info("DONE")
    return cart

  def remove_product_from_cart(cb, cart_id, product_id, amount, request_id):
    log = logger.bind(function="remove_product_to_cart", request_id=request_id, entity_id=cart_id)
    log.info("INCOMING")
    cart = cb.get(cart_id)

    if cart is None:
      return None

    if product_id in cart:
      cart[product_id] = max(0, cart[product_id] - amount)
    else:
      cart[product_id] = amount
    
    cb.put(cart_id, cart)
    log.info("DONE")
    return str(cart)

  def get_cart_contents(cb, cart_id, request_id):
    log = logger.bind(function="get_cart_contents", request_id=request_id, entity_id=cart_id)
    log.info("INCOMING")
    cart = cb.get(cart_id)

    log.info("DONE")
    return cart

  def checkout_get_cart_contents(cb, checkout):
    request_id = checkout.get("request_id")
    cart_id = checkout["cart_id"]
    log = logger.bind(function="checkout_get_cart_contents", request_id=request_id, entity_id=cart_id)
    log.info("INCOMING")
    cart = cb.get(cart_id)

    log.info("DONE")
    return {
      "cart": cart,
      "user_id": checkout["user_id"],
      "request_id": request_id
    }

  cloud.register(add_product_to_cart, "add_product_to_cart")
  cloud.register(remove_product_from_cart, "remove_product_from_cart")
  cloud.register(get_cart_contents, "get_cart_contents")
  cloud.register(checkout_get_cart_contents, "checkout_get_cart_contents")
