# HiveMQ for DC/OS

## Setup

1. Download the following files into `/framework` or replace URLs in `/framework/universe/resource.json`:
```
https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz
https://www.hivemq.com/releases/hivemq-4.0.2.zip
https://downloads.mesosphere.com/java/server-jre-8u162-linux-x64.tar.gz
```
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

When updating the resources of running pods, they will be recreated one after the other, but there will be no
node draining or check for cluster health before killing a node. The next node will only be created once
the previous one is restarted and healthy - so there is a health check performed in between, just not before killing
the first node that is to be updated.