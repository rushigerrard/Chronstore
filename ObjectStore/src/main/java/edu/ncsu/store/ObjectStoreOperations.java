package edu.ncsu.store;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

import edu.ncsu.chord.ChordID;

/**
 * Created by amit on 24/3/17.
 */
public interface ObjectStoreOperations extends Remote {

  /* The getObject, putObject and deleteObject methods are only to be used by
  StoreClientAPIImpl. These should not be used by communication between
  different object stores. The methods for that communication are given below */
  byte[] getObject(ChordID<String> key) throws RemoteException;

  boolean putObject(ChordID<String> key, byte[] value) throws RemoteException;

  boolean delete(ChordID<String> key) throws RemoteException;

  /* These methods are only to be used when objectStore of one node wants to
  communicate with object store of other node. StoreClientImpl should not call
  these methods. The reason for this distinction is that StoreClientAPIImpl does
  not have to worry about the replica number and other metadata. It will simply
  call getObject or PutObject method and then that object store will take care
  of metadata */
  boolean putObjects(Map<KeyMetadata, byte[]> keyValueMap) throws RemoteException;

  boolean makeReplicas(Map<KeyMetadata, byte[]> replicaData) throws RemoteException;

  boolean removeReplica(ChordID<String> key) throws RemoteException;

  List<KeyMetadata> keySet() throws RemoteException;
}
