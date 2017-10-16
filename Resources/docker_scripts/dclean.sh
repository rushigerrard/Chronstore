#!/bin/bash
sudo killall java
sudo rm -rf ~/logs
sudo docker kill $(sudo docker ps -q)
sudo docker rm $(sudo docker ps -a -q)
sudo rm -rf ~/chronstore
sudo rm -rf ~/Dockerfile
