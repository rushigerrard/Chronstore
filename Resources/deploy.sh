#!/bin/bash
set -x

# GITHUB_TOKEN=d5b4a6611363172dce6dee49cf9db293cfac6fde

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 USER IP-ADDRESS" >&2
  exit 1
fi

USER=$1
IP=$2

# copy dockerfile to machine
scp Dockerfile ${USER}@${IP}:/home/${USER}

ssh -t -i ~/.ssh/id_rsa -o "StrictHostKeyChecking=no"  ${USER}@${IP} <<'EOF'

sudo chown -R ${USER} .

echo "Running on: " `hostname`

sleep 2

# clone the git repo
git clone https://d5b4a6611363172dce6dee49cf9db293cfac6fde@github.ncsu.edu/aabarve/chronstore.git

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

# Build the image
sudo docker build -t chronstore .

# Run the container - this should go in drun
sudo docker run -v ~/chronstore:/root/chronstore chronstore

EOF
