from locust import HttpUser, task, between, events
from random import randint
from utils import get_random_product, read_products

products = None

@events.init_command_line_parser.add_listener
def _(parser):
  parser.add_argument("--products-file", required=True)
  parser.add_argument("--products-distribution", default="zipf", choices=["zipf", "uniform"])
  parser.add_argument("--distribution-parameter", type=float)

@events.test_start.add_listener
def _(environment, **_):
  global products
  products = read_products(environment.parsed_options.products_file)

class CheckoutUser(HttpUser):
  wait_time = between(1, 2)

  @task
  def checkout(self):
    if not products:
      print("Products not loaded before test start")
      return

    user_response = self.client.post("/users", json={})
    user = user_response.text.strip("\"")

    self.client.patch(f"/users/{user}/credits/add", json={"credits": 10000}, name="/users/<id>/credits/add")

    shopping_cart_response = self.client.post("/shopping-carts")
    shopping_cart = shopping_cart_response.text.strip("\"")

    for i in range(randint(2, 8)):
      product = get_random_product(products,
                                   self.environment.parsed_options.products_distribution,
                                   self.environment.parsed_options.distribution_parameter)
      amount = randint(1, 4)
      self.client.post(f"/shopping-carts/{shopping_cart}/products", json={"productId": product, "amount": amount}, name="/shopping-carts/<id>/products")

    self.client.post("/orders/checkout", json={"cartId": shopping_cart, "userId": user})
