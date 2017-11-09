#!/bin/bash

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <IP_LIST>" >&2
  exit 0
fi


# set environment variable
export CHRON_BOOTSTRAP_NODELIST=$1

# killall running java porcesses - remove this later
killall java

# Install required packages,
sudo apt-get -y update
sudo apt-get install -y maven git emacs24 \
     apt-transport-https \
     ca-certificates \
     curl \
     gnupg2 \
     software-properties-common \
     build-essential \
     # python-pip \
     # python-dev \

# VCL servers come with java pre-installed ..no need to do it again
# Make debconf update so that orcale java installation wont popup
# terms and conditions aggreement.
# echo debconf shared/accepted-oracle-license-v1-1 select true | \
# sudo debconf-set-selections
# echo debconf shared/accepted-oracle-license-v1-1 seen true | \
# sudo debconf-set-selections
# sudo apt-get install -y oracle-java8-installer


#sudo iptables -A INPUT -i eth1 -j ACCEPT
#sudo iptables -A INPUT -i eth0 -j ACCEPT


echo "Running chord node.."
nohup java -jar -Dlog4j.configuration=log4j.objectstore.properties ObjectStore*.jar </dev/null >/dev/null &

