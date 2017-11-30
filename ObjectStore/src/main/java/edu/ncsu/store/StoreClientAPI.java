package edu.ncsu.store;

import edu.ncsu.chord.ChordID;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by amit on 1/4/17.
 */
public interface StoreClientAPI extends Remote {

  Object get(String key) throws RemoteException;

  List<Object> get(String key, long fromtimestamp, long totimestamp) throws RemoteException;

  Object get(String key, long timestamp) throws RemoteException;

  void put(String key, Object value) throws RemoteException;

  void delete(String key) throws RemoteException;

  List<KeyMetadata> keySet() throws RemoteException;
//  HashMap<String, DataContainer> dumpStore() throws RemoteException;
}