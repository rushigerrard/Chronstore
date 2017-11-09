This project is divided in 3 sub modules
1. Chord module
2. Object Store module
3. Client module

Chord module simply provides a chord layer implementation and can not
be run independently. ObjectStore module and Client module use Chord module as dependency
and provide required services.

Each module is a maven project and can be built into an uber jar. To run start a chord node
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
It is assumed that we already have SSH based access to these machines via above IP addresses.
Following things need to be done in order to setup chord nodes on these 4 machines
1. First, we need to build ObjectStore project into an uber jar (Remember, ObjectStore is
the module which has main method and which runs a particular Chord node)
2. Copy paste this jar on all 4 machines
3. We use centralized logging so that logs of all chord nodes are automatically collected at
a single node (It can be any node but we generally use the very first IP address in the given
list, in this case it will be node A). We need to setup this on that first node.
4. Setup environment variables which are required for ObjectStore process (Currently these are
only used for specifying Bootstrap nodes)

Then we first need to build our jars 
and we already
have ssh access to this machine via user 'tester'.
(To setup everything on current machine just replace the IP with localhost).

