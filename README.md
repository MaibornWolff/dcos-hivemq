# HiveMQ for DC/OS

### Project status
This package is a proof of concept. Since HiveMQ and DC/OS are a natural fit we wanted to explore some possibilities of a HiveMQ Framework. Do not use in production!

## Setup

1. Setup minidcos as per instructions or use your existing cluster
2. [Install dcosdev and setup an asset repository](https://github.com/mesosphere/dcosdev)
3. Run `dcosdev java build` to build the modified scheduler
4. Run `dcosdev up` in /framework to upload the resources
5. Install package from dcos cli or Web UI

## Features

### TLS

Note: To use TLS you need to have an enterprise DC/OS license.

You need to setup a service account with sufficient privilege to create certificates:
```
dcos package install --cli dcos-enterprise-cli
dcos security org service-accounts keypair private-key.pem public-key.pem
dcos security org service-accounts create -p public-key.pem -d "HiveMQ service account" hivemq-principal
dcos security secrets create-sa-secret --strict private-key.pem hivemq-principal hivemq/account-secret
dcos security org groups add_user superusers hivemq-principal
```
Then you can use `hivemq-principal` as service account and `hivemq/account-secret` as service account secret in the
package configuration and enable HiveMQ's TLS support. Certificate generation and signing will be performed by DC/OS itself.

### Decommission Task

The decommission_task will be run whenever a node has to stop or restart. The script currently commented out would first check for the cluster to be healthy and then put the node to be decommissioned
in maintenance mode. Only if the node has no connected clients any more and the cluster is still healthy, will this task
finish and the node be killed. 

Currently, HiveMQ does not provide a health or decomimission endpoint, therefore the decommission_task is without effect (but runs nevertheless). The files status.json and test_decom.sh can be used to develop and test the communication with a future endpoint.

The important part of the scheduler customisation is in CustomizedDecommissionPlanFactory, where an additional 
DeploymentStep is inserted before the task is killed. The decommission_step can therefore easily be moved behind the task killing step if that is the desired behaviour.

In that context please also note, that the customised scheduler will fail if the service definition svc.yml does not contain
a decommission_task task for every node that is defined there. If you want to remove decommissioning altogether, you have
to use the default scheduler. To prevent any downscaling, remove the line `allow-decommission: true` from svc.yml.

### MQTT User Management

If you enable the user management, you can replace the static user configuration file on all nodes:
```
dcos package install --cli --yes hivemq
dcos hivemq plan start add_user -p USERS_FILE="$(cat credentials.xml)"
```

Users will *not* be populated to new nodes. They will start with the default configuration. When user management is enabled,
the official RBAC HiveMQ extension will be enabled and the credentials.xml file will be placed in a persistent location.
The file will be symlinked to the extension folder, so the extension itself can be enabled or disabled, but the credentials
file will not be removed with the extension itself.

You can choose whether to interpret the passwords in credentials.xml as hashed or plain text passwords. For password generation
please refer to the offical [plugin documentation](https://www.hivemq.com/extension/file-rbac-extension/)

### Add Extensions During Runtime

Additional extensions can be added to all running HiveMQ nodes with the following command:
```
dcos hivemq plan start add_plugin -p URL=https://url/to/extension.zip
```
Note that additional configuration is not possible. If you need non-standard configurations, please modify and reupload
the extension ZIP to a public URL.

Extensions will persist during restart and redeploy, but will *not* be transferred to nodes created later.

### Control Center

The control center can be enabled from the user settings and a password for the `admin` user can be set. Additional users
are not supported. To change the admin password, the whole cluster will be redeployed because the user settings are part
of the main HiveMQ configuration. Please also note that MQTT users and control center users are not the same.

### Persistent Volume

All data that is stored in hivemq-data/ persists between restarts and configuration changes. All data stored in this folder
can be accessed from sidecar-plans like add_user and add_plugin. Therefore, extensions are stored there as well. Most
directly supported plugins are removed and reinstalled during node restart in case they are not needed anymore. Manually
installed extensions will persist as well as core HiveMQ data.

The amount of persistent storage can be configured in node.disk, by default 50MB is allocated.

### License

The HiveMQ license can be provided either as a base64 encoded string in the service configuration (hivemq.core_settings.license_file)
or through a DC/OS secret. Please provide the secret name to hivemq.core_settings.license_secret and make sure the secret
accessible for the service.

### Ports and Virtual Network

All exposed ports can be configured and most can be disabled. Make sure that enough DC/OS nodes with all necessary ports 
unused are available. If cannot guarantee that, you can enable the virtual network where every node gets its own IP address.
You cannot move in or out of the virtual network after the first deployment.

Most ports that are exposed also have a load balancing Virtual IP (VIP) configured. The domain of the VIP can be found
(together with all direct connections to individual nodes) in the endpoints section of the created service in the DC/OS
Web UI. For connections from outside the cluster via marathon-lb additional configuration is necessary.

### Readiness and Health Check

Both checks currently only contain a very basic check because HiveMQ does not have an endpoint to communicate its health.

### External Dependencies & Development

The framework automatically downloads HiveMQ 4.0.2 and Java 11.0.2 from their respective sources defined in resource.json. The service will fail
if one of these is not available any more.

Because these external dependencies need to be downloaded every time a node is started, development can be sped up by
downloading them to the cluster's artifact storage (probably minio) and replacing the URLs in resource.json.

### Scheduler Development

If you plan to continue working on the scheduler, it might be worth compiling the scheduler locally (instead of in a Docker
container using `dcosdev build java`). To do that, install a compatible Java (openjdk8) and Gradle (4.8) version and run
`gradle check DistZip` in /framework/java/scheduler. When building locally, all dependencies will be downloaded only once
instead of every time a build is started in Docker.

### Cluster Discovery

For cluster discovery the DC/OS Cluster discovery plugin from [here](https://github.com/MaibornWolff/hivemq-dcos-cluster-discovery-plugin)
is used. It does not require any configuration, as it reads the location of the scheduler from the task environment and
calls the scheduler /endpoints endpoint directly, which is automatically exposed. All discovery ports and IP addresses are
read and returned to HiveMQ. That means, every node could expose the cluster discovery on a random port and they would still
find each other. Currently the cluster discovery port is defined by the user for all nodes. If you struggle to find a port
that is open on all nodes, you can set the discovery port to 0 to generate a random port.

### Disable Clustering

The operation as a cluster can be disabled, which will result in multiple individual HiveMQ servers that are not connected.
This is not recommended as there is no redundancy or failsafe. If you need multiple independent HiveMQs, you can create
multiple clusters as well. If you do so, please make sure that you change the ports of the second cluster or enable the
virtual network if you want to run multiple instances on the same DC/OS nodes.

### Acknowledgements

This framework is based on the DC/OS SDK and was developed using dcosdev. Thanks to Mesosphere for providing these tools.

### Disclaimer

This project is currently not associated with dc-square, the makers of HiveMQ.

This software is provided as-is. Use at your own risk.
