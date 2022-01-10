import argparse
import sys
import os
from uuid import uuid4
from concurrent.futures import ThreadPoolExecutor, ProcessPoolExecutor
from datetime import datetime
import subprocess
import time

# import gevent
# from gevent import monkey
# monkey.patch_all()

import requests
# from locust.env import Environment
# from locust.stats import stats_printer, stats_history

#from users.checkout_user import CheckoutUser
from log_collection import SaveKafkaLogsThread

parser = argparse.ArgumentParser(description="Benchmark client", formatter_class=lambda prog: argparse.HelpFormatter(prog,max_help_position=40))
parser.add_argument("--dist", dest="distribution", default="zipf", choices=["zipf", "uniform"], help="product frequency distribution")
parser.add_argument("--dist-parameter")
parser.add_argument("--products", dest="nr_products", default=100, type=int, help="number of products to generate")
parser.add_argument("--host", dest="host", default="localhost", help="hostname of the system ingress")
parser.add_argument("--port", dest="port", default=80, type=int, help="port on which the system is running")
parser.add_argument("--users", dest="nr_users", default=100, type=int, help="amount of users simulated by locust")
parser.add_argument("--spawn-rate", dest="spawn_rate", default=10, type=int, help="locust user spawn rate")
parser.add_argument("--kafka-host", help="Hostname of the kafka server that contains the cluster logs")
parser.add_argument("--kafka-port", default=9094, type=int, help="Port of the kafka server that contains the cluster logs")
parser.add_argument("--kafka-topic", default="cluster-logs", help="Kafka topic which contains the cluster logs")
parser.add_argument("--compress-logs", help="Output captured logs from kafka to a gzip compressed file", action="store_true")

args = parser.parse_args()

os.makedirs("out/", exist_ok=True)
timestamp = datetime.now().strftime('%Y%m%d-%H%M')

# exc = ProcessPoolExecutor(max_workers=1)
thread = None
# If kafka info provided, start process which saves the logs from kafka
if args.kafka_host:
  print("Saving logs collected from kafka")
  # f = exc.submit(save_kafka_logs, filename=f"out/{timestamp}-logs.txt{'.gz' if args.compress_logs else ''}", hostname=args.kafka_host, port=args.kafka_port, topic=args.kafka_topic, gzip_output=args.compress_logs)
  thread = SaveKafkaLogsThread(filename=f"out/{timestamp}-logs.txt{'.gz' if args.compress_logs else ''}", hostname=args.kafka_host, port=args.kafka_port, topic=args.kafka_topic, gzip_output=args.compress_logs)
  thread.start()
  

# Generate products
print(f"Generating {args.nr_products} products")
products = [str(uuid4()) for _ in range(0, args.nr_products)]
products_filename = f"out/{timestamp}-products.txt"
with open(products_filename, "w") as p:
  for product in products:
    r = requests.patch(f"http://{args.host}:{args.port}/products/{product}", json={"stock": 100000, "price": 10})
    if not r.ok:
      print("HTTP Status: r.status_code")
      sys.exit(r.text)
    p.write(product + "\n")

print(f"Products generated and written to file {products_filename}")

host = f"http://{args.host}:{args.port}"
p = subprocess.run(
  "python -m locust "
  "-f users/checkout_user "
  "--headless "
  "-t 1m " 
  f"-u {args.nr_users} -r {args.spawn_rate} "
  f"--host {host} "
  f"--products-file {products_filename}", 
  shell=True,
  stdout=subprocess.PIPE
)

# env = Environment(user_classes=[CheckoutUser], host=f"http://{args.host}:{args.port}")
# env.create_local_runner()

# # start a greenlet that periodically outputs the current stats
# gevent.spawn(stats_printer(env.stats))
# # start a greenlet that save current stats to history
# gevent.spawn(stats_history, env.runner)

# env.runner.start(args.nr_users, spawn_rate=args.spawn_rate)
# gevent.spawn_later(60, lambda: env.runner.quit())
# env.runner.greenlet.join()
if args.kafka_host:
  print("Collecting remaining logs (1 minute)")
  time.sleep(60)
  thread.terminate()
  thread.join()
