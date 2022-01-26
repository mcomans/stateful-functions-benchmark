from .logging import setup_logging
import structlog

setup_logging()
logger = structlog.get_logger()

def register_users_functions(cloud):
  def add_user_credits(cb, user_id, credits, request_id):
    log = logger.bind(function="add_user_credits", request_id=request_id, entity_id=user_id)
    log.info("INCOMING")
    user = cb.get(user_id)

    if user is None:
      user = {"credits": credits}
    else:
      user["credits"] -= int(credits)
    
    cb.put(user_id, user)
    log.info("DONE")
    return user

  def retract_user_credits(cb, user_id, credits, request_id):
    log = logger.bind(function="retract_user_credits", request_id=request_id, entity_id=user_id)
    log.info("INCOMING")
    user = cb.get(user_id)

    if user is None:
      return None
    else:
      user["credits"] -= int(credits)
    
    cb.put(user_id, user)
    log.info("DONE")
    return user

  def checkout_retract_credits(cb, retract_stock_result):
    request_id = retract_stock_result.get("request_id")
    user_id = retract_stock_result["user_id"]
    log = logger.bind(function="retract_user_credits", request_id=request_id, entity_id=user_id)
    log.info("INCOMING")

    if not retract_stock_result["success"]:
      log.info("Checkout - User retract credits skipped, not enough stock")
      log.info("DONE")
      return {
        "request_id": request_id,
        "rollback_stock": False
      }

    user = cb.get(user_id)
    new_credits = user["credits"] - int(retract_stock_result["total_price"])

    if new_credits < 0:
      log.info("Checkout - Not enough credits")
      log.info("DONE")
      return {
        "rollback_stock": True,
        "cart": retract_stock_result["cart"],
        "request_id": request_id
      }

    user["credits"] = new_credits
    cb.put(user_id, user)

    log.info("Checkout - Checkout successful")
    log.info("DONE")

    return {
      "rollback_stock": False,
      "cart": retract_stock_result["cart"],
      "request_id": request_id
    }

  cloud.register(add_user_credits, "add_user_credits")
  cloud.register(retract_user_credits, "retract_user_credits")
  cloud.register(checkout_retract_credits, "checkout_retract_credits")
