#!/usr/bin/env bash

# Wait until cluster is healthy before putting node into maintenance mode
while true
do
 export STATUS=$(cat status.json)
 echo ${STATUS} | jq
 if $(echo "$STATUS" | jq -e '.overall == "HEALTHY"') ; then
    echo "Cluster is healthy"
    break
 else
    echo "Cluster is NOT healthy"
        sleep 5
 fi
done
# put node into maintenance mode
#curl -H "Content-Type: application/json" -d '{"state": "maintenance","disconnectInterval": "40s","disconnectBatch": "30"}' http://$NODE.$FRAMEWORK_HOST:1234/state
# Wait until all clients are disconnected and the cluster is healthy
while true
do
 export STATUS=$(cat status.json)
 echo ${STATUS} | jq
 export THIS_NODE=$(echo "$STATUS" | jq .currentNode)
 if $(echo "$STATUS" | jq -e '.overall == "HEALTHY"') &&  $(echo "$STATUS" | jq -e ".nodes.$THIS_NODE.connectedClients == 0") ; then
    echo "Cluster is healthy and all clients disconnected"
    break
 else
    echo "Cluster not healthy or clients still connected"
    sleep 5
 fi
done