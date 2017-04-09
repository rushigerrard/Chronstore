package edu.ncsu.store;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.ncsu.chord.ChordID;
import edu.ncsu.chord.UpcallEventHandler;

/**
 * Created by amit on 9/4/17.
 */
public class ChordEventHandler implements UpcallEventHandler {

  /* Keep all loggers transient so that they are not passed over RMI call */
  private final transient static Logger logger = Logger.getLogger(ChordEventHandler.class);

  @Override
  public void handleEvent(ChordID<InetAddress> newPredecessor) {
    logger.info("Handler called. New predecessor is: " + newPredecessor);

    /* Go through all keys of localStorage and see if we have any keys that needs to be
    moved to this new predecessor.*/
    ObjectStore store = ObjectStoreService.getStore();
    ChordID<InetAddress> selfChordID = ObjectStoreService.getChordSession().getChordNodeID();
    ArrayList<String> allKeys = store.keySet();
    Map<ChordID<String>, byte[]> misplacedObjects = new HashMap();
    for (String key : allKeys) {
      ChordID<String> chordKey = new ChordID<>(key);
      if (newPredecessor.inRange(chordKey, selfChordID, true, false)) {
        // This key ID is less than new predecessor ID.
        // This key needs to be moved to new predecessor.
        try {
          misplacedObjects.put(chordKey, store.getObject(chordKey));
        } catch (RemoteException e) {
          e.printStackTrace();
        }
      }
    }
    logger.info("Number of keys that needs to moved: " + misplacedObjects.size());

    // Start key movement. First get remote object for predecessor object store
    ObjectStoreOperations predecessorStore = StoreRMIUtils.getRemoteObjectStore(newPredecessor.getKey());
    try {
      predecessorStore.putObjects(misplacedObjects);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}