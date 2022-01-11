# [Microsoft Orleans](https://dotnet.github.io/orleans/) benchmark implementation

## Deployment to Kubernetes (minikube)
1. Deploy PostgreSQL to kubernetes cluster using Helm:
``` sh
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
helm install postgresql --set postgresqlPassword=benchmark bitnami/postgresql
```

2. Create database schema for Orleans clustering and grain persistence:
``` sh
eval $(minikube -p minikube docker-env)
cd database
docker build -t benchmark-orleans-init .

kubectl run benchmark-orleans-init --image benchmark-orleans-init --restart=Never --image-pull-policy=Never --attach --rm --env="PGPASSWORD=benchmark"
```

3. Build container image and deploy the application:
``` sh
cd ..

docker build -f benchmark.API/Dockerfile -t benchmark-orleans .
kubectl apply -f deployment/role.yaml
kubectl apply -f deployment/deployment.yaml
kubectl apply -f deployment/service.yaml
# kubectl apply -f deployment/ingress.yaml
```
