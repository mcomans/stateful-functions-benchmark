from functions.products import register_products_functions
from functions.users import register_users_functions
from functions.shopping_carts import register_carts_functions

def register_functions(cloud):
  register_products_functions(cloud)
  register_users_functions(cloud)
  register_carts_functions(cloud)