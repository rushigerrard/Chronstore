# Chronstore
This is a simple Immutable and Distributed Key-Value store.

It is a completely distributed peer-to-peer system (No central server).
It provides a simple 'get' & 'put' API to store or retrieve the objects. 
System can easily accomodate new nodes and can efficiently handle existing node failure. 
All data is replicated to prevent data loss. 
The immutable term means, all updates to a particular key will be stored and can be retrieved
at any time if required. In fact, instead of thinking this as a key-value store, it is better
to think of it as a time series database.

Project contains below important directories and files.

./Chord
./ObjectStore
./Resources
./Client
./Documents

Chord - The core chord module which implements chord protcol & exposes API for ObjectStore.
Client - The client modules which contains client library code for get/put API.
ObjectStore - The ObjectStore module which implements key-value store on top of chord and 
exposes get/put API for client.
Resources - A set of shell scripts used mainly for deploying multiple chord nodes of multiple VMs

For instructions on how to run please read INSTALL.md
