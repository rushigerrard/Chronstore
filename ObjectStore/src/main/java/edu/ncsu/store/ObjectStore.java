package edu.ncsu.store;

import edu.ncsu.store.persistence.ImmutableStore;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.*;

import javax.xml.crypto.Data;

import edu.ncsu.chord.ChordDriver;
import edu.ncsu.chord.ChordID;
import edu.ncsu.chord.ChordOperations;
import edu.ncsu.chord.ChordSession;

/**
 * Created by amit on 24/3/17.
 */
class ObjectStore implements ObjectStoreOperations {

  private LocalStorage localStorage;

  /* Keep all loggers transient so that they are not passed over RMI call */
  private final transient static Logger logger = Logger.getLogger(ObjectStore.class);

  public ObjectStore() {
    try {
      localStorage = new ImmutableStore();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /* The flow of get/put key:
  Client will call get/put on StoreClientAPI. The StoreClientAPIImpl will then use chordsession
  to identify the reponsible node and call getObject or putObject on that node using RMI.
  If its a getObject call then that node can simply return the value associated with key. However,
  if that is a putObject call then the node will put the key in KeyMeatdata object along with
  the replicaNumber (which is a metadata field) and then call putObjects method. This method will
  then replicate the data and also store that data in its local store. */
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
      logger.info("Creating first copy of " + key +
                  " on Node: " + ObjectStoreService.getChordSession().getChordNodeID());
      Map<KeyMetadata, byte[]> replicaData = new HashMap<>();
      KeyMetadata km = new KeyMetadata(key);
      km.setReplicaNumber(1);
      replicaData.put(km, value);
      putObjects(replicaData);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  @Override
  public boolean delete(ChordID<String> key) throws RemoteException {
    removeReplica(key);
    return true;
  }



  /* Method for internal usage of objectStore - Not to be used by StoreClientImpl */

  @Override
  public boolean putObjects(Map<KeyMetadata, byte[]> keyValueMap) throws RemoteException {
    boolean result = true;
    makeReplicas(keyValueMap);
    return result;
  }


  @Override
  public boolean makeReplicas(Map<KeyMetadata, byte[]> replicaData) throws RemoteException {
    /* ReplicaData is set of keys that needs to be replicated. However for few of them this node
    might be the last copy node. So separate those keys from the keys that needs to be passed further
     */
    Map<KeyMetadata, byte[]> furtherPassedKeys = new HashMap<>();
    for (KeyMetadata km : replicaData.keySet()) {
      if ((km.replicaNumber + 1) <= StoreConfig.REPLICATION_COUNT) {
        // Increase the replica count before passing it further
        KeyMetadata newKm = new KeyMetadata(km.key);
        newKm.setReplicaNumber(km.replicaNumber + 1);
        furtherPassedKeys.put(newKm, replicaData.get(km));
      }
    }

    if (furtherPassedKeys.size() > 0) {
      ChordID<InetAddress> successorChordID = ObjectStoreService.getChordSession().getSelfSuccessor();
      ObjectStoreOperations successorStore = StoreRMIUtils.getRemoteObjectStore(successorChordID.getKey());
      successorStore.makeReplicas(furtherPassedKeys);
    }

    try {
      StringBuilder log = new StringBuilder("For node: "+ ObjectStoreService.getChordSession().getChordNodeID()+ "\n");
      for (Map.Entry<KeyMetadata, byte[]> e : replicaData.entrySet()) {
        log.append("Putting key :" + e.getKey() + " value: " + e.getValue() + "\n");
        // e.getKey() will return hashMap key of this entry, in our hashmap key is Object of
        // KeyMetadata.
        // e.getKey().key will return key field inside KeyMetadata object. This key field holds
        // chordID<String> object. We put keyMetadata in local store.
        localStorage.put(e.getKey(), e.getValue());
      }
      logger.info(log);
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
    return true;
  }

  @Override
  public boolean removeReplica(ChordID<String> key) throws RemoteException {
    return false;
  }

  @Override
  public List<KeyMetadata> keySet() throws RemoteException {
    return localStorage.keySet();
  }

//  /* this method is written only for testing purposes */
//  public HashMap<String, DataContainer> dumpStore() {
//    return localStorage.dumpStorage();
//  }

}
