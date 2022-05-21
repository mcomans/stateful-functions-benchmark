import numpy as np
from random import randint
from collections.abc import Callable

def read_products(filename: str):
  products = []
  with open(filename, "r") as f:
    products = [r.rstrip() for r in f.readlines()]
  return products


def get_random_product(products: list[str], dist: str, param: float) -> str:
  rand = __random_generator(len(products) - 1, dist, param)
  product_index = rand()
  while product_index >= len(products):
    product_index = rand()

  return products[product_index]

def __random_generator(max: int, dist: str, param: float) -> Callable[[], int]:
  if dist == "uniform":
    return lambda: randint(0, max)
  if dist == "zipf":
    return lambda: np.random.zipf(param or 1.25)
  else:
    return lambda: np.random.zipf(param or 1.25)

__seconds_per_unit = {"s": 1, "m": 60, "h": 3600, "d": 86400, "w": 604800}

def convert_to_seconds(s):
    return int(s[:-1]) * __seconds_per_unit[s[-1]]
