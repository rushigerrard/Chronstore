#!/bin/bash
set -x
sudo apt install maven
USER=rsghatpa
SSH_OPT="-o StrictHostKeyChecking=no"
SCP_OPT="-r -o StrictHostKeyChecking=no"

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <ip-address-file" >&2
  echo "The ip-address file should contain one IP address per line"
  exit 1
fi

LOG_SERVER_IP=""
IP_LIST=""
i=0
ipList=()
while read -r line || [[ -n "$line" ]]; do
    echo "$i: $line"
    IP_LIST=${line}","${IP_LIST}
    ipList[$i]=$line
    ((i++))
done < "$1"
LOG_SERVER_IP=${ipList[0]}
echo "IP LIST: ${IP_LIST}, logging server at : ${LOG_SERVER_IP}"

# Update log.properties file before creating jar
sed s/localhost/${LOG_SERVER_IP}/g ../Chord/src/main/resources/log4j.properties > ../Chord/src/main/resources/log4j.chord.properties
sed s/localhost/${LOG_SERVER_IP}/g ../ObjectStore/src/main/resources/log4j.properties > ../ObjectStore/src/main/resources/log4j.objectstore.properties
sed s/localhost/${LOG_SERVER_IP}/g ../Client/src/main/resources/log4j.properties > ../Client/src/main/resources/log4j.client.properties


echo "Building Chord.."
cd ../Chord
mvn clean install

echo "Building ObjectStore.."
cd ../ObjectStore
mvn clean install
mvn clean compile -DskipTests=true assembly:single

echo "Building client libraries.."
cd ../Client
mvn clean compile assembly:single


cd ../Resources
numIP=${#ipList[@]}
for (( i=0; i<${numIP}; i++ ));
do
    IP=${ipList[$i]}
    echo "setting up: ${IP}"

    # copy the iplist file to all machines
    scp ${SCP_OPT} $1 ${USER}@${IP}:nodes
    # copy log properties for server
    scp ${SCP_OPT} log_server ${USER}@${IP}:
    # copy jar files
    scp ${SCP_OPT} ../Client/target/client*.jar ${USER}@${IP}:
    scp ${SCP_OPT} ../ObjectStore/target/ObjectStore*.jar ${USER}@${IP}:
    ssh ${SSH_OPT} ${USER}@${IP} 'bash -s' < setup.sh ${IP_LIST}
    
    # setup logging server on very first ip
    if [ $i -eq 0 ]
    then
	ssh ${SSH_OPT} ${USER}@${IP} 'bash -s' <<EOF

echo "Starting log server on first node.."
cd log_server

# Get log4j jar
wget -O log4j-1.2.17.jar http://central.maven.org/maven2/log4j/log4j/1.2.17/log4j-1.2.17.jar

# start all 3 logging services
nohup java -cp log4j-1.2.17.jar org.apache.log4j.net.SimpleSocketServer 4712 log4j.Chord.properties </dev/null >/dev/null &
nohup java -cp log4j-1.2.17.jar org.apache.log4j.net.SimpleSocketServer 4713 log4j.KeyStore.properties </dev/null >/dev/null &
nohup java -cp log4j-1.2.17.jar org.apache.log4j.net.SimpleSocketServer 4714 log4j.Analysis.properties </dev/null >/dev/null &
EOF
    fi
done

# connect to first node and start log server

# echo "USER: ${USER}, IP: ${IP}, BRANCH: ${BRANCH}"

# copy dockerfile to machine
# scp Dockerfile ${USER}@${IP}:/home/${USER}

# copy the chronstore source into host machine
















