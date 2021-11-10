From the [cloudstate documentation](https://cloudstate.io/docs/deploy/install-production.html):

1. Create namespace and deploy operator
```shell
kubectl create namespace cloudstate
kubectl apply -n cloudstate -f https://github.com/cloudstateio/cloudstate/releases/download/v0.6.0/cloudstate-0.6.0.yaml
```


