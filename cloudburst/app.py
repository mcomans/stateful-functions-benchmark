from flask import Flask
from functions import register_functions
from cloud import cloud

register_functions(cloud)

app = Flask(__name__)
app.url_map.strict_slashes = False

from routes import orders, products, shopping_carts, users

app.register_blueprint(orders, url_prefix="/orders")
app.register_blueprint(products, url_prefix="/products")
app.register_blueprint(shopping_carts, url_prefix="/shopping-carts")
app.register_blueprint(users, url_prefix="/users")
