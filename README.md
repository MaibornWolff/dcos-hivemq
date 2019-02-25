# HiveMQ for DC/OS

## Setup

2. Setup minidcos as per instructions or use your existing cluster
3. Install dcosdev and setup an asset repository https://github.com/mesosphere/dcosdev
3. Run `dcosdev java build` to build the modified scheduler
4. Run `dcosdev up` in /framwork to upload the resources
5. Install package

### TLS

Note: To use TLS you need to have an enterprise DC/OS license.

You need to setup a service account with sufficient privilege to create certificates:
```
dcos security org service-accounts keypair private-key.pem public-key.pem
dcos security org service-accounts create -p public-key.pem -d "HiveMQ service account" hivemq-principal
dcos security secrets create-sa-secret --strict private-key.pem hivemq-principal hivemq/account-secret
dcos security org groups add_user superusers hivemq-principal
```
Then you can use `hivemq-principal` as service account and `hivemq/account-secret` as service account secret in the
package configuration and enable HiveMQ's TLS support.

### Decommission Task

The decommission task will run before a HiveMQ node should be stopped when updating the configuration to a lower number
of nodes. Right now, it will first check for the cluster to be healthy and then put the node to be decommissioned
in maintenance mode. Only if the node has no connected clients any more and the cluster is still healthy, will this task
finish and the node be killed. 

### Update Pod Resources

When updating the resources of running pods, they will be recreated one after the other. Before replacing a node, the
cluster needs to be healthy and the node will be put in maintenance mode. Only when all clients have disconnected and
the cluster is still healthy, will the node be replaced.

### Add users

If you enable the user management, you can add users during runtime with this command:
```
dcos hivemq plan start add_user -p USER=Alwin -p PASSWORD=test -p ROLE=superuser
```

Users will not persist during pod restart or redeploy.