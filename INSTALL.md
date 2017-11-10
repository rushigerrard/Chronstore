This project is divided in 3 sub modules
1. Chord module
2. Object Store module
3. Client module

Chord module simply provides a chord layer implementation and can not
be run independently. ObjectStore module and Client module use Chord module as dependency
and provide required services.

Each module is a maven project and can be built into a jar. To run a chord node
(which can store key-value pairs and be a part of existing chord network) we have to build
ObjectStore module into an uber jar and run that jar. Similarly, to use this chord networks
(i.e to put or retrieve key-value pairs or to test this project) Client module needs to be
built into an uber jar.

We provide a set of scripts which will allow you to deploy this complete system on multiple 
machines and then test it. Note that we can NOT run multiple chord nodes on same 
OS (To do that we need to run JAVA RMI on per OS basis than on per node basis - future
improvement), However, we can easily create multiple VMs or docker containers and run the 
whole network on single physical host. The set of scripts provided with this project allow
you to deloy this on multiple VMs or multiple physical hosts (only IP address and ssh access
to these machines are required)

These scripts currently only work for Debian/Ubuntu based systems (However, converting them
for other Linux systems should be easy)

SETUP
======
Let us assume that we want to deploy this system on 4 machines A,B,C,D with IP address 
as follows:
[A=192.168.0.101, B=192.168.0.102, C=192.168.0.103, D=192.168.0.104] 
It is assumed that we already have SSH based access to these machines via above IP addresses and these 4 machines are able to communicate with each other using these IP addresses.

Following things need to be done in order to setup chord nodes on these 4 machines
1. First, we need to build ObjectStore project into an uber jar (Remember, ObjectStore is
the module which has main method and which runs a particular Chord node)
2. Copy paste this jar on all 4 machines
3. We use centralized logging so that logs of all chord nodes are automatically collected at
a single node (It can be any node but we generally use the very first IP address in the given
list, in this case it will be node A). We need to setup this on that first node.
4. Setup environment variables which are required for ObjectStore process (Currently these are
only used for specifying Bootstrap nodes)

We provide a script which does all of these things: 

First cd into Resources directory inside project folder. This directory has a script named
'deploy.sh'. Open that script, at the top it contains a 'USER' variable. This variable is 
used while sshing into above machines. Change the value of that variable to the one which
should be used for 'ssh'.
Next, create a new file named 'nodelist' in same directory. In that file enter all 4 IP addresses 
one per line. 'deploy.sh' reads this file to get all IP addresses on which the code should be 
deployed. 
Now run the script as follows:

./deploy.sh nodelist

'nodelist' is passed as the argumnet to 'deploy.sh'. This should do all the setup and deploy 
chronstore on all 4 nodes. Note: This script must be run from the Resources directory itself. 
If you don't 'cd' into Resources directory before running the script, then script will fail.


USING
========
For using this chornstore, a client library is provided. This library is provided in Client 
project. The Client project also contains an example 'Client.java' code which uses this library
and does 'get'/'put' operations on chronstore. For easier testing we provide a set of randomly 
generated 1000 key-value pairs. The Client.java can be used to put some of these keys into
chronstore. 

We can run Client.java from local machine. First 'cd' into Resources directory. In this directory
we have already created a 'nodelist' file in previous step. This directory also has a script named
'run_client' which should be used for running the client with given key-values. This script takes
4 input parameters as follows:
1. GET/PUT/TEST: One of these three strings should be passed as the first command line argument to
the script. 'GET' will make the client run 'get(key)' operations on the chronstore, 'PUT' will make
the client run 'put(key, value)' operations on the chronstore, 'TEST' will make the client go into
each node of chronstore and verify its key-value pairs. 
2. NUMBER: Second command line argument should be the number of keys for which one of the above 
three operations should be done. This value must be between 0-1000.
3. nodelist: third command line argument is the 'nodelist' file which contains the IP addresses of 
all nodes participating in the chronstore.
4. testkey-file: The name of file in which randomly generated key-value pairs are stored. 
Such example file is provided in 'tests/input_keys' directory under Resources file.

So let us say we run our run_client.sh as follows:

./run_client PUT 100 nodelist tests/input_keys

Above command means, PUT 100 key-value pairs into chronstore, using the IP addresses given in
'nodelist' file, and read these 100 key-value pairs from file 'tests/input_keys'.


Note: Doing TEST before doing any PUT is
going to fail, test only validates the keys from input file with actual stored data on nodes, but
if there is not data present on node, then it will always fail.
Note: The input_keys file has a specific format, you can create your own input_keys by following 
this format. The format is as follows:
  
ID$<key>$<value>

This file should contain one key-value pair per line. The first entity in a line is the key number
i.e if the file has 1000-keys then this will be between 0-1000, Then a '$' as a separator, then 
next entity is the actual key, then agina a '$' as a separator followed by the value for this key.

When we run client with 100 as second argumnt, it reads 0-100 keys from this file. If our first
argument to run_client is PUT then it will 'put' these 100 key-value pairs into chronstore, on 
the other hand if our first argument is 'GET' then it will use these first 100 keys and do a 
'get' operation on chronstore and then match the values returned by chronstore with the values
present in file.


Finally, to kill all nodes and clear everything from those VMs run (from Resources directory)

./kill_all.sh nodelist

This will remove all the data stored on the VMs, clear the logs and kill all node processes.