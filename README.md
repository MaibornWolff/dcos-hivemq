# HiveMQ for DC/OS

## Setup

1. Download the following files into `/framework` or replace URLs in `/framework/universe/resource.json`:

```
https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz
https://www.hivemq.com/releases/hivemq-4.0.2.zip
https://downloads.mesosphere.com/java/server-jre-8u162-linux-x64.tar.gz
https://ecosystem-repo.s3.amazonaws.com/sdk/artifacts/0.55.2/operator-scheduler.zip
```

2. Setup minidcos as per instructions or use your existing cluster
3. Install dcosdev and setup an asset repository https://github.com/mesosphere/dcosdev
4. Run `dcosdev up` in /framwork to upload the resources
5. Install package

###TLS
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