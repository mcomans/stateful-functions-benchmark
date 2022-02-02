from .logging import setup_logging
import structlog
import logging
import orjson


def register_products_functions(cloud):
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

  class SetProductPrice:
    def __init__(self, cb):
      setup_logging()
      self.logger = structlog.get_logger()

    def run(self, cb, product_id, price, request_id):
      log = self.logger.bind(function="set_product_price", request_id=request_id, entity_id=product_id)
      log.info("INCOMING")
      product = cb.get(product_id)
      if product is None:
        product = {"price": price, "stock": 0}
      else:
        product["price"] = price

      cb.put(product_id, product)
      log.info("DONE")
      return product

  class RetractProductStock:
    def __init__(self, cb):
      setup_logging()
      self.logger = structlog.get_logger()

    def run(self, cb, product_id, amount, request_id):
      log = self.logger.bind(function="retract_product_price", request_id=request_id, entity_id=product_id)
      log.info("INCOMING")
      product = cb.get(product_id)
      if product is None:
        return None
      else:
        product["stock"] -= int(amount)

      cb.put(product_id, product)
      log.info("DONE")
      return product

  def add_product_stock(logger, cb, product_id, amount, request_id):
    log = logger.bind(function="add_product_stock", request_id=request_id, entity_id=product_id)
    log.info("INCOMING")
    product = cb.get(product_id)
    if product is None:
      product = {"price": 0, "stock": amount}
    else:
      product["stock"] += int(amount)

    cb.put(product_id, product)
    log.info("DONE")
    return product

  class AddProductStock:
    def __init__(self, cb):
      setup_logging()
      self.logger = structlog.get_logger()

    def run(self, cb, product_id, amount, request_id):
      return add_product_stock(self.logger, cb, product_id, amount, request_id)

  class CheckoutRetractAllStock:
    def __init__(self, cb):
      setup_logging()
      self.logger = structlog.get_logger()

    def run(self, cb, cart_result):
      request_id = cart_result.get("request_id")
      log = self.logger.bind(function="checkout_retract_all_stock", request_id=request_id)
      log.info("INCOMING")
      log.info("Checkout - Retracting all stock")

      cart = cart_result["cart"]
      results = [self.checkout_retract_stock(cb, product_id, amount, request_id) for product_id, amount in cart.items()]

      failures = [not r["success"] for r in results if r]

      if any(failures):
        print("Rollback started - Not enough stock")
        _ = [add_product_stock(self.logger, cb, r["product_id"], r["amount"], request_id) for r in results if r and r["success"]]

        return {
          "success": False,
          "request_id": request_id
        }

      total = sum([int(r["amount"]) * int(r["price"]) for r in results if r])

      log.info("DONE")
      return {
        "cart": cart,
        "success": True,
        "total_price": total,
        "user_id": cart_result["user_id"],
        "request_id": request_id
      }

    def checkout_retract_stock(self, cb, product_id, amount, request_id):
      log = self.logger.bind(function="checkout_retract_stock", request_id=request_id, entity_id=product_id)
      log.info("INCOMING")
      product = cb.get(product_id)

      if product is not None:
        new_stock = product["stock"] - int(amount)
        if (new_stock < 0):
          return {"success": False}

        product["stock"] = new_stock

        cb.put(product_id, product)

        log.info("DONE")
        return {
          "success": True,
          "product_id": product_id,
          "amount": amount,
          "price": product["price"]
        }

  class CheckoutRollbackStock:
    def __init__(self, cb):
      setup_logging()
      self.logger = structlog.get_logger()

    def run(self, cb, retract_credit_result):
      request_id = retract_credit_result.get("request_id")
      log = self.logger.bind(function="checkout_rollback_stock", request_id=request_id)
      log.info("INCOMING")

      if not retract_credit_result["rollback_stock"]:
        log.info("DONE")
        return {
          "rolled_back": False,
          "cart": retract_credit_result["cart"],
          "request_id": request_id
        }

      log.info("Checkout - Rollback stock, not enough credit")
      cart = retract_credit_result["cart"]
      _ = [add_product_stock(self.logger, cb, product_id, amount, request_id) for product_id, amount in cart.items()]

      log.info("DONE")
      return {
        "rolled_back": True,
        "request_id": request_id
      }


  class CheckoutUpdateAllFreqItems:
    def __init__(self, cb):
      setup_logging()
      self.logger = structlog.get_logger()

    def checkout_update_freq_items(self, cb, product_id, items, request_id):
      product = cb.get(product_id)
      log = self.logger.bind(function="checkout_update_freq_items", request_id=request_id, entity_id=product_id)
      log.info("INCOMING")
      if product is not None:
        freq_items = product.get("freq_items") or {}

        for product in items:
          value = freq_items.get(product) or 0
          freq_items[product] = value

        product["freq_items"] = freq_items

        cb.put(product_id, product)
      log.info("DONE")

    def run(self, cb, rollback_result):
      request_id = rollback_result.get("request_id")
      log = self.logger.bind(function="checkout_update_all_freq_items", request_id=request_id)
      log.info("INCOMING")
      if rollback_result["rolled_back"]:
        return

      cart = rollback_result["cart"]

      for item in cart:
        self.checkout_update_freq_items(cb, item, [i for i in cart if i != item], request_id)

        log.info("DONE")


  class GetTopFreqItem:
    def __init__(self, cb):
      setup_logging()
      self.logger = structlog.get_logger()

    def run(self, cb, query):
      request_id = query.get("request_id")
      product_id = query.get("product_id")
      log = self.logger.bind(function="get_top_freq_item", request_id=request_id, entity_id=product_id)
      log.info("INCOMING")
      if not product_id:
        log.info("DONE")
        return query

      product = cb.get(product_id)

      freq_items = product.get("freq_items")

      if not freq_items:
        log.info("DONE")
        return {
          "product_id": None,
          "found_items": query.get("found_items"),
          "request_id": request_id
        }

      sorted_items = sorted(freq_items.items(), key=lambda item: item[1])
      found_items = query.get("found_items") or []

      for item in reversed(sorted_items):
        if item not in found_items:
          found_items.append(item)
          log.info("DONE")
          return {
            "product_id": item,
            "found_items": found_items,
            "request_id": request_id
          }


      log.info("DONE")
      return {
        "product_id": None,
        "found_items": query.get("found_items"),
        "request_id": request_id
      }


  cloud.register((SetProductPrice, ()), "set_product_price")
  cloud.register((RetractProductStock, ()), "retract_product_stock")
  cloud.register((AddProductStock, ()), "add_product_stock")
  cloud.register((CheckoutRetractAllStock, ()), "checkout_retract_all_stock")
  cloud.register((CheckoutRollbackStock, ()), "checkout_rollback_stock")
  cloud.register((CheckoutUpdateAllFreqItems, ()), "checkout_update_all_freq_items")

  for i in range(1, 50):
    cloud.register((GetTopFreqItem, ()), "get_top_freq_item_" + str(i))
