def set_product_price(cb, product_id, price):
  product = cb.get(product_id);
  if product is None:
    product = {"price": price, "stock": 0}
  else:
    product["price"] = price
  
  cb.put(product_id, product)
  return product

def retract_product_stock(cb, product_id, amount):
  product = cb.get(product_id);
  if product is None:
    return None
  else:
    product["stock"] -= amount
  
  cb.put(product_id, product)
  return product

def add_product_stock(cb, product_id, amount):
  product = cb.get(product_id);
  if product is None:
    product = {"price": 0, "stock": amount}
  else:
    product["stock"] -= amount
  
  cb.put(product_id, product)
  return product

def register_products_functions(cloud):
  cloud.register(set_product_price, "set_product_price")
  cloud.register(retract_product_stock, "retract_product_stock")
  cloud.register(add_product_stock, "add_product_stock")