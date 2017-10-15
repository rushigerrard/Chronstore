package edu.ncsu.store;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.*;

import edu.ncsu.chord.ChordID;
import edu.ncsu.chord.Event;
import edu.ncsu.chord.UpcallEventHandler;

/**
 * Created by amit on 9/4/17.
 */
public class ChordEventHandler implements UpcallEventHandler {

  /* Keep all loggers transient so that they are not passed over RMI call */
  private final transient static Logger logger = Logger.getLogger(ChordEventHandler.class);

  public void moveKeystoNewPredecessor(ChordID<InetAddress> prevPredecessor,
				       ChordID<InetAddress> newPredecessor) {
    logger.info("Handler called in " + ObjectStoreService.getChordSession().getChordNodeID()
		+ " New predecessor is: " + newPredecessor);

      /* Go through all keys of localStorage and see if we have any keys that needs to be
      moved to this new predecessor.*/
    ObjectStore store = ObjectStoreService.getStore();
    try {
    List<KeyMetadata> allKeys = store.keySet();
    Map<KeyMetadata, byte[]> misplacedObjects = new HashMap();
    for (KeyMetadata km : allKeys) {
      if (km.replicaNumber == 1 &&
          km.key.inRange(prevPredecessor, newPredecessor, false, true)) {
        // This key ID is either a replica or belongs to new predecessor
        // This key needs to be moved to new predecessor.

          misplacedObjects.put(km, store.getObject(km.key));
      }
    }

    logger.info("Number of keys that needs to moved: " + misplacedObjects.size());
    // TODO: remove below log statement after debugging is done
    logger.info("About to move below keys: " + new ArrayList<>(misplacedObjects.keySet()));
    // Start key movement. First get remote object for predecessor object store
    ObjectStoreOperations
	predecessorStore =
	StoreRMIUtils.getRemoteObjectStore(newPredecessor.getKey());
      predecessorStore.putObjects(misplacedObjects);
      // If above operation did not throw an exception
      // only then delete those keys from your storage
      //store.deleteKeys(new ArrayList<>(misplacedObjects.keySet()));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void moveKeystoNewSuccessor(ChordID<InetAddress> prevSuccessor,
                                       ChordID<InetAddress> newSuccessor) {
    // move all the keys with replicaNumber != StoreConfig.REPLICATION_COUNT to this new successor
    logger.info("Handler called in " + ObjectStoreService.getChordSession().getChordNodeID()
                + " New successor is: " + newSuccessor);

    /* Go through all keys of localStorage and see if we have any keys that needs to bb
      further replicated.*/
    ObjectStore store = ObjectStoreService.getStore();
    if (store == null) {
      logger.info("Store Object found to be NULL. No further action needed!");
      return;
    }

    try {
    List<KeyMetadata> allKeys = store.keySet();
    Map<KeyMetadata, byte[]> replicableKeys = new HashMap();
    for (KeyMetadata km : allKeys) {
      if (km.replicaNumber != StoreConfig.REPLICATION_COUNT) {
        // This key ID needs be further replicated
        KeyMetadata newKm = new KeyMetadata(km.key);
        newKm.setReplicaNumber(km.replicaNumber + 1);
          replicableKeys.put(newKm, store.getObject(km.key));
      }
    }
    logger.info("Number of keys that can be replicated: " + replicableKeys.size());
    // TODO: remove below log statement after debugging is done
    logger.info("About to replicate below keys: " + new ArrayList<>(replicableKeys.keySet()));
    // Start key movement. First get remote object for predecessor object store
    ObjectStoreOperations
        successorStore =
        StoreRMIUtils.getRemoteObjectStore(newSuccessor.getKey());

      successorStore.makeReplicas(replicableKeys);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void handleSuccessorFailure(ChordID<InetAddress> prevPredecessor,
                                     ChordID<InetAddress> newPredecessor) {
    /**
     * When successor fails, you need to call replicateKeys on all the keys with
     * replicaNumber < StoreConfig.REPLICA_COUNT (Keys with replicaNumber == REPLICA_COUNT
     * have no relation with successor)
     */

  }

  public void replicateKeysofFailedPredecessor(ChordID<InetAddress> prevPredecessor,
                                     ChordID<InetAddress> newPredecessor) {
    /** replicateKeysofFailedPredecessor:
     *  When predecessor fails, you need to call replicate Keys on all the
     *  keys with replicaNumber == 2. (keys with replicaNumber == 2 are the keys
     *  which had failed predecessor as primary node)
     *  */
    // move all the keys with replicaNumber != StoreConfig.REPLICATION_COUNT to this new successor
    logger.info("Handler called in " + ObjectStoreService.getChordSession().getChordNodeID()
                + " failed predecessor is: " + prevPredecessor);

    /* Go through all keys of localStorage and see if we have any keys that needs to bb
      further replicated.*/
    ObjectStore store = ObjectStoreService.getStore();
    try {
    List<KeyMetadata> allKeys = store.keySet();
    Map<KeyMetadata, byte[]> replicableKeys = new HashMap();
    for (KeyMetadata km : allKeys) {
      /* Second replicas are the keys whose primary node failed - now you are the primary node for those */
      if (km.replicaNumber == 2 ) {
        // This key ID can be further replicated
        km.replicaNumber = 1;
        // Create new keyMetadata for replication
        KeyMetadata newKm = new KeyMetadata(km.key);
        newKm.setReplicaNumber(km.replicaNumber + 1);

          replicableKeys.put(newKm, store.getObject(km.key));

      }
    }
    logger.info("Number of keys that can be replicated: " + replicableKeys.size());
    // TODO: remove below log statement after debugging is done
    logger.info("About to replicate below keys: " + new ArrayList<>(replicableKeys.keySet()));
    // Start key movement. First get remote object for predecessor object store
    ObjectStoreOperations successorStore =
        StoreRMIUtils.getRemoteObjectStore(ObjectStoreService.getChordSession().getSelfSuccessor().getKey());

      successorStore.makeReplicas(replicableKeys);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /* preValue and newValue will contain old and updated values of predecessor/successor depending on the event
  * Currently we can Identify only 4 types of important events with respect to key movements
  * 1.SUCCESSOR_FAILED,
  * 2.NEW_SUCCESSOR,
  * 3.PREDECESSOR_FAILED,
  * 4.NEW_PREDECESSOR
  *
  * Note a SUCCESSOR FAILED or PREDECSSOR_FAILED even will most probably be always followed by
  * NEW_SUCCESSOR or NEW_PREDECESSOR event so if key movement is being done on these events then you can
  * avoid doing any keymovement on SUCCSSOR_FAILED or PREDECESSOR_FAILED events (just do all key movement on
  * NEW_* events.
  * Ideally on getting NEW_PREDECSSOR you move all the keys that now belong to that predecessor and on getting
  * NEW_SUCCESSOR you call replicate on all the kesy that have replciaNumber < REPLICA_COUNT
  * */
  @Override
  public void handleEvent(Event updateEvent,
                          ChordID<InetAddress> prevValue, ChordID<InetAddress> newValue) {
    switch (updateEvent) {
      case NEW_PREDECESSOR: {
        moveKeystoNewPredecessor(prevValue, newValue);
        break;
      }
      case NEW_SUCCESSOR: {
        moveKeystoNewSuccessor(prevValue, newValue);
        break;
      }
      case PREDECESSOR_FAILED: {
        replicateKeysofFailedPredecessor(prevValue, newValue);
        break;
      }
      case SUCCESSOR_FAILED: {
        moveKeystoNewSuccessor(prevValue, newValue);
        break;
      }
      default:
        break;
    }
  }
}
