from locust import HttpUser, task, constant, events, LoadTestShape
from random import randint
from utils import get_random_product, read_products, convert_to_seconds

import math

products = None

@events.init_command_line_parser.add_listener
def _(parser):
  parser.add_argument("--products-file", required=True)
  parser.add_argument("--products-distribution", default="zipf", choices=["zipf", "uniform"])
  parser.add_argument("--distribution-parameter", type=float)
  parser.add_argument("--step-load-users", action="store_true")
  parser.add_argument("--step-duration", default=100, type=int, help="Amount of time for each step, if step load is chosen")
  parser.add_argument("--step-user-count", default=10, type=int, help="Amount of users spawned per step, if step load is chosen")
  parser.add_argument("--load-run-time", default="1m", help="Custom run time")
  parser.add_argument("--load-max-users", default=60, help="Max users to spawn", type=int)
  parser.add_argument("--load-spawn-rate", default=60, help="Spawn rate to use if not using step load", type=int)

@events.test_start.add_listener
def _(environment, **_):
  global products
  products = read_products(environment.parsed_options.products_file)

@events.init.add_listener
def set_load_parameters(environment, **_):
  global num_users
  num_users = environment.parsed_options.load_max_users

  global run_time
  run_time = convert_to_seconds(environment.parsed_options.load_run_time)

  global step_load
  step_load = environment.parsed_options.step_load_users

  print(step_load)

  if step_load:
    global step_time
    step_time = environment.parsed_options.step_duration
    global step_users
    step_users = environment.parsed_options.step_user_count
  else:
    global spawn_rate
    spawn_rate = environment.parsed_options.load_spawn_rate


class CheckoutUser(HttpUser):
  wait_time = constant(1)

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

class StepLoadShape(LoadTestShape):
    def tick(self):
        current_run_time = self.get_run_time()

        if current_run_time > run_time:
            return None

        if step_load:
          current_step = math.floor(current_run_time / step_time) + 1
          return (min(current_step * step_users, num_users), step_users)
        else:
          return (num_users, spawn_rate)
