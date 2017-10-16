#!/bin/bash
set -x

for i in `seq 1 $1`
do
    echo "$i"
    sudo docker run -v ~/chronstore:/root/chronstore chronstore &
done
