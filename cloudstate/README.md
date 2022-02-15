# [Cloudstate](https://cloudstate.io) benchmark implementation

## Deployment to kubernetes (minikube):

1. Create namespace and deploy operator:
```sh
kubectl create namespace cloudstate
kubectl apply -n cloudstate -f k8s/cloudstate.yaml
```

2. Deploy sidecar roles:
``` sh
kubectl apply -f k8s/sidecar-roles.yaml
```

3. Build services and API:

``` sh
eval $(minikube -p minikube docker-env)

./gradlew jibDockerBuild
```

4. Deploy services and API:
``` sh
kubectl apply -f k8s/product-service.yaml
kubectl apply -f k8s/shoppingcart-service.yaml
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/order-service.yaml

kubectl apply -f k8s/api.yaml
```
