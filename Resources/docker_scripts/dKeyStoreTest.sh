#!/bin/bash

set -x

# generate a list of IP addresses of all containers
sudo python ~/DockerImage/keyvalue/Resources/docker_scripts/FetchIPAddress.py

# run maven tests
sudo docker run -v ~/DockerImage/keyvalue/Resources/tests/:/root/KeyValueStore/Resources/tests/ keyvalue bash -c "cd /root/KeyValueStore/Client;mvn exec:java -Dexec.args=\"$1 $2\""