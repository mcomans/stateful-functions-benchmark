def register_products_functions(cloud):
  def set_product_price(cb, product_id, price):
    product = cb.get(product_id)
    if product is None:
      product = {"price": price, "stock": 0}
    else:
      product["price"] = price

    cb.put(product_id, product)
    return product

  def retract_product_stock(cb, product_id, amount):
    product = cb.get(product_id)
    if product is None:
      return None
    else:
      product["stock"] -= int(amount)
    
    cb.put(product_id, product)
    return product

  def add_product_stock(cb, product_id, amount):
    product = cb.get(product_id)
    if product is None:
      product = {"price": 0, "stock": amount}
    else:
      product["stock"] += int(amount)
    
    cb.put(product_id, product)
    return product

  def checkout_retract_stock(cb, product_id, amount):
      product = cb.get(product_id)

      if product is not None:
        new_stock = product["stock"] - int(amount)
        if (new_stock < 0):
          return {"success": False}

        product["stock"] = new_stock

        cb.put(product_id, product)

        return {
          "success": True,
          "product_id": product_id,
          "amount": amount,
          "price": product["price"]
        }

  def checkout_retract_all_stock(cb, cart_result):
    print("Checkout - Retracting all stock")
    cart = cart_result["cart"]
    results = [checkout_retract_stock(cb, product_id, amount) for product_id, amount in cart.values()]

    failures = [not r["success"] for r in results]

    if any(failures):
      print("Rollback started - Not enough stock")
      _ = [add_product_stock(cb, r["product_id"], r["amount"]) for r in results if r["success"]]

      return {
        "success": False
      }

    total = sum([int(r["amount"]) * int(r["price"]) for r in results])

    return {
      "cart": cart,
      "success": True,
      "total_price": total,
      "user_id": cart_result["user_id"]
    }

  def checkout_rollback_stock(cb, retract_credit_result):
    if not retract_credit_result["rollback_stock"]:
      return

    print("Checkout - Rollback stock, not enough credit")
    cart = retract_credit_result["cart"]
    _ = [add_product_stock(cb, product_id, amount) for product_id, amount in cart.values()]

    return

  cloud.register(set_product_price, "set_product_price")
  cloud.register(retract_product_stock, "retract_product_stock")
  cloud.register(add_product_stock, "add_product_stock")
  cloud.register(checkout_retract_all_stock, "checkout_retract_all_stock")
  cloud.register(checkout_rollback_stock, "checkout_rollback_stock")
