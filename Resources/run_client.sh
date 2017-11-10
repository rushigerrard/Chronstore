#!/bin/bash

# set -x


# run maven tests
java -jar -Dlog4j.properties=log4j.client.properties ../Client/target/client*.jar $1 $2 $3 $4
# sudo docker run -v ~/chronstore:/root/chronstore chronstore bash -c "cd /root/chronstore/Chord;mvn clean install;cd /root/chronstore/ObjectStore;mvn clean install;cd /root/chronstore/Client;mvn compile exec:java -Dexec.args=\"$1 $2\""
