# Test cluster deployment

## Full kubernetes cluster

..

## Minikube

1. Start minikube and deploy kafka
```
minikube start --memory 8000 --disk-size 15000 --cpus 4
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
helm install kafka bitnami/kafka --set externalAccess.enabled=true --set maxMessageBytes=_10000000
```
2. From another shell, run `minikube tunnel` to be able to use a load balancer external service.
3. Update kafka deployment so it knows which external IP to use
```
LOAD_BALANCER_IP_1="$(kubectl get svc --namespace default kafka-0-external -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
helm upgrade --namespace default kafka bitnami/kafka \
      --set replicaCount=1 \
      --set externalAccess.enabled=true \
      --set 'externalAccess.service.loadBalancerIPs[0]'=$LOAD_BALANCER_IP_1 \
      --set externalAccess.service.type=LoadBalancer \
      --set maxMessageBytes=_10000000
```
4. Deploy fluentd DaemonSet:
```
kubectl apply -f k8s/fluentd-config.yaml
kubectl apply -f k8s/fluentd.yaml
```
5. Deploy the to-be-tested system to cluster according to system-specific instructions
