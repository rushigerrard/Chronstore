#!/bin/bash
sudo docker kill $(sudo docker ps -q)
sudo docker rm $(sudo docker psa -q)