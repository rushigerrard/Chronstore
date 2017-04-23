#!/bin/bash
echo "Removing running/dead docker instances"
dkill.sh
sudo docker build -f ~/DockerImage/Dockerfile -t keyvalue:latest ~/DockerImage/