from google.cloud.monitoring_v3.query import Query
from google.cloud.monitoring_v3 import MetricServiceClient
from google.cloud import monitoring

def download_resource_data(start_time, end_time, project, out_prefix):
  client = MetricServiceClient()

  pod_cpu_usage = Query(client,
                project,
                metric_type="prometheus.googleapis.com/container_cpu_usage_seconds_total/counter") \
                .select_interval(end_time=end_time, start_time=start_time) \
                .align(monitoring.Aggregation.Aligner.ALIGN_RATE, seconds=10) \
                .select_resources(namespace="default") \
                .reduce(monitoring.Aggregation.Reducer.REDUCE_SUM, 'metric.pod')
  pod_cpu_usage.as_dataframe().to_csv(out_prefix + "_pod_cpu_usage.csv")

  node_cpu_usage = Query(client,
                project,
                metric_type="prometheus.googleapis.com/container_cpu_usage_seconds_total/counter") \
                .select_interval(end_time=end_time, start_time=start_time) \
                .align(monitoring.Aggregation.Aligner.ALIGN_RATE, seconds=10) \
                .reduce(monitoring.Aggregation.Reducer.REDUCE_SUM, 'metric.node')
  node_cpu_usage.as_dataframe().to_csv(out_prefix + "_node_cpu_usage.csv")

  pod_memory_usage = Query(client,
                project,
                metric_type="prometheus.googleapis.com/container_memory_working_set_bytes/gauge") \
                .select_interval(end_time=end_time, start_time=start_time) \
                .select_resources(namespace="default") \
                .align(monitoring.Aggregation.Aligner.ALIGN_MEAN, seconds=10) \
                .reduce(monitoring.Aggregation.Reducer.REDUCE_SUM, 'metric.pod')

  pod_memory_usage.as_dataframe().to_csv(out_prefix + "_pod_memory_usage.csv")

  node_memory_usage = Query(client,
                project,
                metric_type="prometheus.googleapis.com/container_memory_working_set_bytes/gauge") \
                .select_interval(end_time=end_time, start_time=start_time) \
                .align(monitoring.Aggregation.Aligner.ALIGN_MEAN, seconds=10) \
                .reduce(monitoring.Aggregation.Reducer.REDUCE_SUM, 'metric.node')

  node_memory_usage.as_dataframe().to_csv(out_prefix + "_node_memory_usage.csv")
