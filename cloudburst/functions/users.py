def register_users_functions(cloud):
  def add_user_credits(cb, user_id, credits):
    user = cb.get(user_id)

    if user is None:
      user = {"credits": credits}
    else:
      user["credits"] -= int(credits)
    
    cb.put(user_id, user)
    return user

  def retract_user_credits(cb, user_id, credits):
    user = cb.get(user_id)

    if user is None:
      return None
    else:
      user["credits"] -= int(credits)
    
    cb.put(user_id, user)
    return user

  def checkout_retract_credits(cb, retract_stock_result):
    if not retract_stock_result["success"]:
      print("Checkout - User retract credits skipped, not enough stock")
      return {
        "rollback_stock": False
      }

    user_id = retract_stock_result["user_id"]
    user = cb.get(user_id)

    if user is None:
      return None

    new_credits = user["credits"] - int(retract_stock_result["total_price"])

    if new_credits < 0:
      print("Checkout - Not enough credits")
      return {
        "rollback_stock": True,
        "cart": retract_stock_result["cart"]
      }

    user["credits"] = new_credits
    cb.put(user_id, user)

    print("Checkout - Checkout successful")

    return {
      "rollback_stock": False,
      "cart": retract_stock_result["cart"]
    }

  cloud.register(add_user_credits, "add_user_credits")
  cloud.register(retract_user_credits, "retract_user_credits")
  cloud.register(checkout_retract_credits, "checkout_retract_credits")
