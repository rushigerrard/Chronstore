#!/bin/bash
# set -x

# GITHUB_TOKEN=d5b4a6611363172dce6dee49cf9db293cfac6fde

if [ "$#" -ne 3 ]; then
  echo "Usage: $0 USER IP-ADDRESS BRANCH" >&2
  exit 1
fi

USER=$1
IP=$2
BRANCH=$3

echo "USER: ${USER}, IP: ${IP}, BRANCH: ${BRANCH}"

# copy dockerfile to machine
# scp Dockerfile ${USER}@${IP}:/home/${USER}

ssh -t -i ~/.ssh/id_rsa -o "StrictHostKeyChecking=no" ${USER}@${IP} IP=${IP} BRANCH=${BRANCH} 'bash -s' << 'EOF'

echo "USER: ${USER}, IP: ${IP}, BRANCH: ${BRANCH}"

sudo chown -R ${USER} .

echo "Running on: " `hostname` "with user: ${USER}"

sleep 2

# clone the git repo
git clone https://d5b4a6611363172dce6dee49cf9db293cfac6fde@github.ncsu.edu/aabarve/chronstore.git
cd chronstore
git checkout ${BRANCH}

# STEP: install docker
# add repo
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install -y maven git emacs24 \
     apt-transport-https \
     ca-certificates \
     curl \
     gnupg2 \
     software-properties-common \
     docker-ce

cd ..

mkdir logs
# Get config files into logs
cp chronstore/Resoruces/log_server/* logs/
cd logs

# Get log4j jar
wget http://central.maven.org/maven2/log4j/log4j/1.2.17/log4j-1.2.17.jar

# start all 3 logging services
java -cp log4j-1.2.17.jar org.apache.log4j.net.SimpleSocketServer 4712 log4j.Chord.properties &
java -cp log4j-1.2.17.jar org.apache.log4j.net.SimpleSocketServer 4713 log4j.KeyStore.properties &
java -cp log4j-1.2.17.jar org.apache.log4j.net.SimpleSocketServer 4714 log4j.Analysis.properties &

cd ..

# Change the hostname in log4j.properties files 
sed -i.bak s/loghost/${IP}/g chronstore/Chord/src/main/resources/log4j.properties
sed -i.bak s/loghost/${IP}/g chronstore/ObjectStore/src/main/resources/log4j.properties
sed -i.bak s/loghost/${IP}/g chronstore/Client/src/main/resources/log4j.properties


# get Dockerfile
cp chronstore/Resources/Dockerfile .

# Build the image
sudo docker build -t chronstore .

# Run the container - this should go in drun
# sudo docker run -v ~/chronstore:/root/chronstore chronstore


EOF
