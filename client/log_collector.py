from kafka import KafkaConsumer
import gzip
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("--host", help="Kafka hostname", default="localhost")
parser.add_argument("--port", help="Kafka port", default=9094, type=int)
parser.add_argument("--topic", default="cluster-logs", help="Kafka topic which contains the cluster logs")
parser.add_argument("--out", required=True, help="File where the logs are saved")
parser.add_argument("--compress", help="Output captured logs from kafka to a gzip compressed file", action="store_true")

args = parser.parse_args()

def save_kafka_logs(filename: str, topic: str, hostname: str, port: str, gzip_output: bool):
  consumer = KafkaConsumer(topic, bootstrap_servers=[f"{hostname}:{port}"])
  if gzip_output:
    with gzip.open(filename, "wb") as out:
      for message in consumer:
        out.write(message.value)
        out.write(b'\n')
  else:
    with open(filename, "wb") as out:
      for message in consumer:
        out.write(message.value)
        out.write(b'\n')

consumer = KafkaConsumer(args.topic, bootstrap_servers=[f"{args.host}:{args.port}"])
out = gzip.open(args.out, "wb") if args.compress else open(args.out, "wb")

for message in consumer:
  out.write(message.value)
  out.write(b'\n')
