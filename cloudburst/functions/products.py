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
    results = [checkout_retract_stock(cb, product_id, amount) for product_id, amount in cart.items()]

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
      return {
        "rolled_back": False,
        "cart": retract_credit_result["cart"]
      }

    print("Checkout - Rollback stock, not enough credit")
    cart = retract_credit_result["cart"]
    _ = [add_product_stock(cb, product_id, amount) for product_id, amount in cart.items()]

    return {
      "rolled_back": True
    }

  def checkout_update_freq_items(cb, product_id, items):
    product = cb.get(product_id)
    if product is not None:
      freq_items = product.get("freq_items") or {}

      for product in items:
        value = freq_items.get(product) or 0
        freq_items[product] = value

      product["freq_items"] = freq_items

      cb.put(product_id, product)

  def checkout_update_all_freq_items(cb, rollback_result):
    if rollback_result["rolled_back"]:
      return

    cart = rollback_result["cart"]

    for item in cart:
      checkout_update_freq_items(cb, item, [i for i in cart if i != item])


  cloud.register(set_product_price, "set_product_price")
  cloud.register(retract_product_stock, "retract_product_stock")
  cloud.register(add_product_stock, "add_product_stock")
  cloud.register(checkout_retract_all_stock, "checkout_retract_all_stock")
  cloud.register(checkout_rollback_stock, "checkout_rollback_stock")
  cloud.register(checkout_update_all_freq_items, "checkout_update_all_freq_items")
