#!/bin/bash

set -x

# generate a list of IP addresses of all containers
sudo python ~/chronstore/Resources/docker_scripts/FetchIPAddress.py

# run maven tests
sudo docker run -v ~/chronstore:/root/chronstore chronstore bash -c "cd /root/chronstore/Chord;mvn clean install;cd /root/chronstore/ObjectStore;mvn clean install;cd /root/chronstore/Client;mvn compile exec:java -Dexec.args=\"$1 $2\""
