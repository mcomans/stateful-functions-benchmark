from kafka import KafkaConsumer
import gzip
import time

from threading import Thread

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

class SaveKafkaLogsThread(Thread):
  def __init__(self, filename: str, topic: str, hostname: str, port: str, gzip_output: bool, *args, **kwargs):
      super().__init__(*args, **kwargs)
      self.topic = topic
      self.filename = filename
      self.hostname = hostname
      self.port = port
      self.gzip_output = gzip_output
      self.terminated = False

  def run(self):
    consumer = KafkaConsumer(self.topic, bootstrap_servers=[f"{self.hostname}:{self.port}"])
    out = gzip.open(self.filename, "wb") if self.gzip_output else open(self.filename, "wb")

    while True:
      if self.terminated:
        break

      tps = consumer.poll()
      for tp_records in tps.values():
        for message in tp_records:
          out.write(message.value) 
          out.write(b'\n')
      time.sleep(1)

    out.close()
  
  def terminate(self):
    self.terminated = True
