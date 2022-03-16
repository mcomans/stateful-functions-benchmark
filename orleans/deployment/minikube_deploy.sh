helm install postgresql --set auth.postgresPassword=benchmark bitnami/postgresql
eval $(minikube -p minikube docker-env)
cd ../database

docker build -t benchmark-orleans-init .
kubectl run benchmark-orleans-init --image benchmark-orleans-init --restart=Never --image-pull-policy=Never --attach --rm --env="PGPASSWORD=benchmark"

cd ..

docker build -f benchmark.API/Dockerfile -t benchmark-orleans .
kubectl apply -f deployment/role.yaml
kubectl apply -f deployment/deployment.yaml
kubectl apply -f deployment/service.yaml
