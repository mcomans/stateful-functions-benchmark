from locust import HttpUser, task, between, events
from locust.runners import MasterRunner
import numpy as np
from random import randint

def read_products(filename):
  products = []
  with open(filename, "r") as f:
    products = [r.rstrip() for r in f.readlines()]
  return products

products = None

@events.init_command_line_parser.add_listener
def _(parser):
  parser.add_argument("--products-file", required=True)

@events.test_start.add_listener
def _(environment, **_kwargs):
  global products
  products = read_products(environment.parsed_options.products_file)

class CheckoutUser(HttpUser):
  wait_time = between(1, 2)

  @task
  def checkout(self):
    user_response = self.client.post("/users", json={})
    user = user_response.text.strip("\"")

    self.client.patch(f"/users/{user}/credits/add", json={"credits": 10000}, name="/users/<id>/credits/add")

    shopping_cart_response = self.client.post("/shopping-carts")
    shopping_cart = shopping_cart_response.text.strip("\"")

    for i in range(randint(2, 8)):
      # TODO: Zipf parameter
      product_index = np.random.zipf(1.5)
      while product_index >= len(products):
        product_index = np.random.zipf(1.5)

      product = products[product_index]
      amount = randint(1, 4)
      self.client.post(f"/shopping-carts/{shopping_cart}/products", json={"productId": product, "amount": amount}, name="/shopping-carts/<id>/products")

    self.client.post("/orders/checkout", json={"cartId": shopping_cart, "userId": user})
