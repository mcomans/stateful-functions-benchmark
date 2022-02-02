import structlog
import logging
import orjson

def register_carts_functions(cloud):
  def setup_logging():
    structlog.configure(
        cache_logger_on_first_use=True,
        wrapper_class=structlog.make_filtering_bound_logger(logging.INFO),
        processors=[
            structlog.threadlocal.merge_threadlocal,
            structlog.processors.add_log_level,
            structlog.processors.format_exc_info,
            structlog.processors.TimeStamper(fmt="iso", utc=True),
            structlog.processors.JSONRenderer(serializer=orjson.dumps),
        ],
        logger_factory=structlog.BytesLoggerFactory(),
    )

  class AddProductToCart:
    def __init__(self, cb):
      setup_logging()
      self.logger = structlog.get_logger()

    def run(self, cb, cart_id, product_id, amount, request_id):
      log = self.logger.bind(function="add_product_to_cart", request_id=request_id, entity_id=cart_id)
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

  class RemoveProductFromCart:
    def __init__(self, cb):
      setup_logging()
      self.logger = structlog.get_logger()

    def run(self, cb, cart_id, product_id, amount, request_id):
      log = self.logger.bind(function="remove_product_to_cart", request_id=request_id, entity_id=cart_id)
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

  class GetCartContents:
    def __init__(self, cb):
      setup_logging()
      self.logger = structlog.get_logger()

    def run(self, cb, cart_id, request_id):
      log = self.logger.bind(function="get_cart_contents", request_id=request_id, entity_id=cart_id)
      log.info("INCOMING")
      cart = cb.get(cart_id)

      log.info("DONE")
      return cart

  class CheckoutGetCartContents:
    def __init__(self, cb):
      setup_logging()
      self.logger = structlog.get_logger()

    def run(self, cb, checkout):
      request_id = checkout.get("request_id")
      cart_id = checkout["cart_id"]
      log = self.logger.bind(function="checkout_get_cart_contents", request_id=request_id, entity_id=cart_id)
      log.info("INCOMING")
      cart = cb.get(cart_id)

      log.info("DONE")
      return {
        "cart": cart,
        "user_id": checkout["user_id"],
        "request_id": request_id
      }

  cloud.register((AddProductToCart, ()), "add_product_to_cart")
  cloud.register((RemoveProductFromCart, ()), "remove_product_from_cart")
  cloud.register((GetCartContents, ()), "get_cart_contents")
  cloud.register((CheckoutGetCartContents, ()), "checkout_get_cart_contents")
