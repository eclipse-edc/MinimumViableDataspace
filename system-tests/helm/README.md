# Helm Charts for MVD

[./helm-charts](./helm-charts) folder contains the helm charts for kubernetes components of the applications used in MVD.

## Mounting Resources
All the necessary resources needed by these applications are in the directory [./k8s_resources](./k8s_resources).
When working with  kubernetes, resources inside this folder have to be mounted to location ```/var/lib/minikube/mvd-resources```
in the host system. For example, the commands to mount in ```minikube``` are,

```minikube mount ./system-tests/helm/k8s_resources/:/var/lib/minikube/mvd-resources```

Or, to mount at start,

```minikube start --mount --mount-string="./system-tests/helm/k8s_resources/:/var/lib/minikube/mvd-resources"```


## Creating Docker Images
As some of the mvd applications do not have publicly available images, we have to create images for those in the host system. 
That is we have to build docker images directly inside minikube. For which we will be using the [docker-compose.yml](./docker-compose.yml) file. 

To point your terminal’s docker-cli to minikube's Docker Engine, run:
```eval $(minikube docker-env)``` 

Then execute ```docker compose -f docker-compose.yml build```

It will create the following docker images in minikube's docker environment: 
```registration-service:v.01```, ```edc-connector:v.01```, ```cli-tools:v.01```, ```edc-connector-dashboard:v.01```.


## Running MVD
To run the MVD, execute ```./run-mvd.sh```
The file [run-mvd.sh](./run-mvd.sh) contains command to install the helm charts that will deploy the kubernetes components in the cluster.


## Testing Endpoints
For now, we have exposed the connectors to the following ports: company1:```30091```, company2:```30092```, company3:```30093```.

We can test the following endpoints,
#### Assets endpoint
[http://<cluster-ip>:30092/api/management/v2/assets/request]()

#### Federated catalog endpoint
[http://<cluster-ip>:30092/api/management/federatedcatalog]()














