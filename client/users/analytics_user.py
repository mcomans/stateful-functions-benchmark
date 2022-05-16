from locust import HttpUser, task, between, events
from random import randint
from utils import get_random_product, read_products
import math

products = None

@events.init_command_line_parser.add_listener
def _(parser):
  parser.add_argument("--products-file", required=True)
  parser.add_argument("--products-distribution", default="zipf", choices=["zipf", "uniform"])
  parser.add_argument("--distribution-parameter", type=float)

@events.test_start.add_listener
def _(environment, **_kwargs):
  global products
  products = read_products(environment.parsed_options.products_file)

class AnalyticsUser(HttpUser):
  wait_time = between(1, 2)

  @task
  def linear_query(self):
    top = 1
    depth = randint(1, 20)

    self.__send_query(top, depth)

  @task
  def fan_out_query(self):
    depth = 2
    top = randint(2, 20)

    self.__send_query(top, depth)


  @task
  def exponential_fan_out_query(self):
    top = randint(2, 20)
    depth = randint(3, math.floor(math.log(1000, top)) + 1)

    self.__send_query(top, depth)

  def __send_query(self, top, depth):
    if not products:
      print("Products not loaded before test start")
      return

    product = get_random_product(products,
                                  self.environment.parsed_options.products_distribution,
                                  self.environment.parsed_options.distribution_parameter)

    self.client.get(f"/products/{product}/freq-items", params={"top": top, "depth": depth}, name="/products/{productId}/freq-items")
