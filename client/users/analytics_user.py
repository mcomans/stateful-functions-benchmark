from locust import HttpUser, task, between, events
from random import randint
from utils import get_random_product

def read_products(filename):
  products = []
  with open(filename, "r") as f:
    products = [r.rstrip() for r in f.readlines()]
  return products

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
  def get_frequent_items(self):
    if not products:
      print("Products not loaded before test start")
      return

    product = get_random_product(products,
                                   self.environment.parsed_options.products_distribution,
                                   self.environment.parsed_options.distribution_parameter)
    
    # draw a random number from a zipf distribution

    # TODO: Choose top and depth parameters
    top = randint(1, 3)
    depth = randint(3, 5)


    self.client.get(f"/products/{product}/freq-items", params={"top": top, "depth": depth}, name="/products/{productId}/freq-items")
