#!/bin/bash

SSH_OPT="-o StrictHostKeyChecking=no"
SCP_OPT="-r -o StrictHostKeyChecking=no"
USER=aabarve

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <ip-address-file" >&2
  echo "The ip-address file should contain one IP address per line"
  exit 1
fi

i=0
ipList=()
while read -r line || [[ -n "$line" ]]; do
    echo "$i: $line"
    IP_LIST=${line}","${IP_LIST}
    ipList[$i]=$line
    ((i++))
done < "$1"

numIP=${#ipList[@]}
for (( i=0; i<${numIP}; i++ ));
do
    IP=${ipList[$i]}
    echo "kill process at: ${IP}"
    ssh ${SSH_OPT} ${USER}@${IP} <<EOF
killall java
rm -rf log_server *.jar nodes
rm -rf /tmp/data/* /tmp/indexes/*
EOF
done
