from uuid import uuid4
from flask import Flask, request, g
from functions import register_functions
from cloud import cloud

import structlog
import orjson
import logging

structlog.configure(
    cache_logger_on_first_use=True,
    wrapper_class=structlog.make_filtering_bound_logger(logging.INFO),
    processors=[
        structlog.threadlocal.merge_threadlocal,
        structlog.processors.add_log_level,
        structlog.processors.format_exc_info,
        structlog.processors.TimeStamper(fmt="iso", utc=True),
        structlog.processors.JSONRenderer(serializer=orjson.dumps),
    ],
    logger_factory=structlog.BytesLoggerFactory(),
)

logger = structlog.get_logger()

register_functions(cloud)

wz = logging.getLogger("werkzeug")
wz.setLevel(logging.ERROR)

app = Flask(__name__)
app.url_map.strict_slashes = False

from routes import orders, products, shopping_carts, users

@app.before_request
def log_before():
  request_id = str(uuid4())
  structlog.threadlocal.clear_threadlocal()
  structlog.threadlocal.bind_threadlocal(
    url=str(request.path),
    rule=str(request.url_rule),
    request_id=request_id,
    method=request.method
  )
  log = logger.bind()
  log.info("INCOMING")

  g.request_id = request_id

@app.after_request
def log_after(f):
  log = logger.bind()
  log.info("DONE")
  return f


app.register_blueprint(orders, url_prefix="/orders")
app.register_blueprint(products, url_prefix="/products")
app.register_blueprint(shopping_carts, url_prefix="/shopping-carts")
app.register_blueprint(users, url_prefix="/users")
