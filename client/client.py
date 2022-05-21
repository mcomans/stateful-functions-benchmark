from google_cloud_monitoring import download_resource_data

import argparse
import sys
import os
from uuid import uuid4
from datetime import datetime
import subprocess
import time
import shutil

import requests

def checkout_benchmark(args, file_prefix):
  # Generate products
  print(f"Generating {args.nr_products} products")
  products = [str(uuid4()) for _ in range(0, args.nr_products)]
  products_filename = f"{file_prefix}-products.txt"
  with open(products_filename, "w") as p:
    for product in products:
      r = requests.patch(f"http://{args.host}:{args.port}/products/{product}", json={"stock": 100000, "price": 10})
      if not r.ok:
        print("HTTP Status: r.status_code")
        sys.exit(r.text)
      p.write(product + "\n")

  print(f"Products generated and written to file {products_filename}")

  print("Calling locust with checkout benchmark")
  host = f"http://{args.host}:{args.port}"
  spawn_workers_and_master(
    "users/checkout_user",
    f"{file_prefix}_locust",
    host,
    products_filename,
    args
  )

def analytics_benchmark(args, file_prefix):
  products_out_filename = f"{file_prefix}-products.txt"
  shutil.copyfile(args.products_file, products_out_filename)
  print(f"Products copied and written to file {products_out_filename}")

  print("Calling locust with analytics benchmark")
  host = f"http://{args.host}:{args.port}"
  spawn_workers_and_master(
    "users/analytics_user",
    f"{file_prefix}_locust",
    host,
    products_out_filename,
    args
  )

def spawn_workers_and_master(locustfile, csv_out, host, products_file, args):
  for i in range(args.workers):
    print(f"Spawning locust worker {i + 1}")
    subprocess.Popen([
      "python", "-m", "locust",
      "-f", locustfile,
      "--load-run-time", args.time,
      "--load-max-users", str(args.nr_users),
      "--load-spawn-rate", str(args.spawn_rate),
      "--step-load-users" if args.step_load else "",
      "--step-user-count", str(args.step_users),
      "--step-duration", str(args.step_time),
      "--host", host,
      "--products-file", products_file,
      "--products-distribution", args.dist,
      "--worker"])

  subprocess.run([
    "python", "-m", "locust",
    "-f", locustfile,
    "--headless",
    f"--csv={csv_out}",
    "--load-run-time", args.time,
    "--load-max-users", str(args.nr_users),
    "--load-spawn-rate", str(args.spawn_rate),
    "--step-load-users" if args.step_load else "",
    "--step-user-count", str(args.step_users),
    "--step-duration", str(args.step_time),
    "--host", host,
    "--products-file", products_file,
    "--products-distribution", args.dist,
    "--master"])

parser = argparse.ArgumentParser(description="Benchmark client", formatter_class=lambda prog: argparse.HelpFormatter(prog,max_help_position=40))

sub_parsers = parser.add_subparsers(title="benchmarks", description="benchmark to run", required=True, dest="benchmark")

parent_parser = argparse.ArgumentParser(add_help=False)
parent_parser.add_argument("--dist", default="zipf", choices=["zipf", "uniform"], help="product frequency distribution")
parent_parser.add_argument("--host", dest="host", default="localhost", help="hostname of the system ingress")
parent_parser.add_argument("--port", dest="port", default=80, type=int, help="port on which the system is running")
parent_parser.add_argument("--users", dest="nr_users", default=100, type=int, help="max amount of users simulated by locust")
parent_parser.add_argument("--spawn-rate", dest="spawn_rate", default=10, type=int, help="locust user spawn rate")
parent_parser.add_argument("--step-load", action="store_true", help="spawn locust users in steps instead of gradually")
parent_parser.add_argument("--step-users", default=20, type=int, help="locust users spawned per step, if --step-load is chosen")
parent_parser.add_argument("--step-time", default=100, type=int, help="time per step, if --step-load is chosen")
parent_parser.add_argument("--kafka-host", help="Hostname of the kafka server that contains the cluster logs")
parent_parser.add_argument("--kafka-port", default=9094, type=int, help="Port of the kafka server that contains the cluster logs")
parent_parser.add_argument("--kafka-topic", default="cluster-logs", help="Kafka topic which contains the cluster logs")
parent_parser.add_argument("--compress-logs", help="Output captured logs from kafka to a gzip compressed file", action="store_true")
parent_parser.add_argument("--time", "-t", default="1m", help="Runtime for each locust user, e.g. 1m, 1h, etc.")
parent_parser.add_argument("--workers", "-w", default=1, type=int, help="Number of locust workers to spawn, one for each available CPU core is advised")
parent_parser.add_argument("--gcloud-project", help="google cloud project_id. If supplied resource data will be downloaded")

checkout_parser = sub_parsers.add_parser("checkout", help="run checkout benchmark", parents=[parent_parser])
checkout_parser.add_argument("--products", dest="nr_products", default=100, type=int, help="number of products to generate")
checkout_parser.set_defaults(func=checkout_benchmark)

analytics_parser = sub_parsers.add_parser("analytics", help="run analytics benchmark", parents=[parent_parser])
analytics_parser.add_argument("--products-file", required=True, help="path to file with product ids")
analytics_parser.set_defaults(func=analytics_benchmark)

args = parser.parse_args()

timestamp = datetime.now().strftime('%Y%m%d-%H%M')
out_folder = f"out/{timestamp}-{args.benchmark}"
os.makedirs(out_folder, exist_ok=True)
file_prefix = f"{out_folder}/{timestamp}-{args.benchmark}"

log_collection_process = None

# If kafka info provided, start thread which saves the logs from kafka
if args.kafka_host:
  print("Saving logs collected from kafka")
  log_collection_process = subprocess.Popen(["python", "log_collector.py",
                    "--host", args.kafka_host,
                    "--port", str(args.kafka_port),
                    "--topic", args.kafka_topic,
                    "--compress" if args.compress_logs else "",
                    "--out", f"{file_prefix}-logs.txt{'.gz' if args.compress_logs else ''}"])

start = datetime.now()
args.func(args, file_prefix)
end = datetime.now()

if args.gcloud_project:
  download_resource_data(start, end, args.gcloud_project, file_prefix)

if log_collection_process:
  print("Collecting remaining logs (1 minute)")
  time.sleep(60)
  log_collection_process.kill()
