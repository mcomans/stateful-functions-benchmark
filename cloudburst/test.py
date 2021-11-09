from cloud import cloud
import numpy as np

def register(cloud):
  def print_x(cb, x):
    print(f"print_x running with x={x}")
    return x

  def fan_out(cb):
    return np.array(["1","2","3"])

  def fan_in(cb, *args):
    print(f"fan_in running with args={args}")
    for a in args:
      print(a)
    return args

  cloud.register(print_x, "print_1")
  cloud.register(print_x, "print_2")
  cloud.register(print_x, "print_3")

  cloud.register(fan_out, "fan_out")
  cloud.register(fan_in, "fan_in")
  cloud.register_dag("test_dag", ["fan_out", "print_1", "print_2", "print_3", "fan_in"], [("fan_out", "print_1"), ("fan_out", "print_2"), ("fan_out", "print_3"), ("print_1", "fan_in"), ("print_2", "fan_in"), ("print_3", "fan_in")])



print("registering functions")
register(cloud)
print("calling dag")
result = cloud.call_dag("test_dag", {})
for a in result.get():
  print(a)
cloud.register_dag