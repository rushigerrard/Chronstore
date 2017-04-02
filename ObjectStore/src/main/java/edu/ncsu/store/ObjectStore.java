package edu.ncsu.store;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import edu.ncsu.chord.ChordDriver;
import edu.ncsu.chord.ChordID;
import edu.ncsu.chord.ChordOperations;
import edu.ncsu.chord.ChordSession;

/**
 * Created by amit on 24/3/17.
 */
public class ObjectStore implements ObjectStoreOperations {

  LocalStorage localStorage;


  /* Keep all loggers transient so that they are not passed over RMI call */
  private final transient static Logger logger = Logger.getLogger(ObjectStore.class);

  public ObjectStore() {
    localStorage = new HashStorage();
  }

  @Override
  public byte[] getObject(ChordID<String> key) throws RemoteException {
    if (!localStorage.containsKey(key.getKey())) {
      return null;
    } else {
      return localStorage.get(key.getKey());
    }
  }

  @Override
  public boolean putObject(ChordID<String> key, byte[] value) throws RemoteException {
    try {
      localStorage.put(key.getKey(), value);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  @Override
  public boolean putObjects(HashMap<ChordID<String>, byte[]> keyValueMap) throws RemoteException {
    boolean result = true;
    for (Map.Entry<ChordID<String>, byte[]> e : keyValueMap.entrySet()) {
      result &= putObject(e.getKey(), e.getValue());
    }
    return result;
  }

  @Override
  public boolean delete(ChordID<String> key) throws RemoteException {
    localStorage.delete(key.getKey());
    return true;
  }

  @Override
  public boolean deleteKeys(HashSet<ChordID<String>> key) throws RemoteException {
    for (ChordID<String> k : key) {
      delete(k);
    }
    return true;
  }

}