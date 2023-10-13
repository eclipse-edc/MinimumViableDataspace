# Helm Charts for MVD
We have demonstrated a containerized deployment of the MVD in [system-tests/README.md](../README.md). To take it one step further 
we will be using [kubernetes](https://kubernetes.io/docs/home/) which is a container orchestration tool for automated deployment, scaling, and management of these containers.
In addition, we have used [Helm](https://helm.sh/docs/) to manage all the Kubernetes YAML files.
[./helm-charts](./helm-charts) folder contains the helm charts for the kubernetes components of the applications used in MVD.


## Deploying MVD
For the deployment purpose we need,
* a kubernetes cluster, and
* helm installed 

You can use [minikube](https://minikube.sigs.k8s.io/docs/) or [kind](https://kind.sigs.k8s.io/) 
to set up a local kubernetes cluster for this testing purpose. Follow the [documentation](https://minikube.sigs.k8s.io/docs/start/) 
to install minikube in your machine. Once everything is installed, you can start the cluster with ```minikube start``` command.
Also, we will be using Helm, which can be installed following the [instructions](https://helm.sh/docs/intro/install/) provide in their official website.


## Mounting Resources
All the necessary resources needed by these applications are in the directory [./k8s_resources](./k8s_resources).
Mount these resources to location ```/var/lib/minikube/mvd-resources```
in the cluster host system. For example, the commands to mount in ```minikube``` will be,

```minikube mount ./system-tests/helm/k8s_resources/:/var/lib/minikube/mvd-resources```

Or, to mount at start,

```minikube start --mount --mount-string="./system-tests/helm/k8s_resources/:/var/lib/minikube/mvd-resources"```


## Creating Docker Images
As some of the mvd applications do not have publicly available images, we have to create images for those in the cluster host system. 
That is we have to build docker images directly inside minikube/kind. For which we will be using the [docker-compose.yml](./docker-compose.yml) file. 

First, edit the path to mvd-datadashboard project in the [docker-compose.yml](./docker-compose.yml) file.
```yaml
  edc-connector-dashboard:
    build:
      #e.g. /home/user/RegistrationService/launcher
      context: path/to/mvd-datadashboard
    image: edc-connector-dashboard:v.01
```

To point your terminal’s docker-cli to minikube's Docker Engine, run:
```eval $(minikube docker-env)``` 

Then execute ```docker compose -f docker-compose.yml build```

It will create the following docker images in minikube's docker environment: 
```registration-service:v.01```, ```edc-connector:v.01```, ```cli-tools:v.01```, ```edc-connector-dashboard:v.01```.


## Running MVD
To run the MVD, execute ```./run-mvd.sh```.
The file [run-mvd.sh](./run-mvd.sh) contains commands to install the helm charts that will deploy the kubernetes components in the cluster.


## Testing Endpoints
For now, we have exposed the connectors to the following ports: company1:```30091```, company2:```30092```, company3:```30093```.

We can test the following endpoints,
#### Assets endpoint
[http://<cluster-ip>:30092/api/management/v2/assets/request]()

#### Federated catalog endpoint
[http://<cluster-ip>:30092/api/management/federatedcatalog]()














