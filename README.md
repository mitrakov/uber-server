# Uber Server
## How to build and deploy

If necessary, bump up version in:
- MainApp.scala

Build:
```shell script
sbt assembly
```

Create Docker image:
```shell script
docker build -t mitrakov/uber-server .
```

Login to DockerHub and push:
```shell script
docker push mitrakov/uber-server
```

Deploy to k8s:
```shell script
kubectl apply -f k8s/uber-service.yaml
kubectl apply -f k8s/uber-deployment.yaml
```

If the deployment already exists and you just want to restart, run:
```shell script
kubectl delete pod uber-65689499b7-hhrml
```
