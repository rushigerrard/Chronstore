package edu.ncsu.store;

import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.*;

import edu.ncsu.chord.ChordID;
import edu.ncsu.chord.ChordSession;

/**
 * Created by amit on 1/4/17.
 */
public class StoreClientAPIImpl implements StoreClientAPI {

  /* Keep all loggers transient so that they are not passed over RMI call */
  private final transient static Logger logger = Logger.getLogger(StoreClientAPIImpl.class);


  private static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream os = new ObjectOutputStream(out);
    os.writeObject(obj);
    return out.toByteArray();
  }

  private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    if (data == null || data.length == 0)
      return null;
    ByteArrayInputStream in = new ByteArrayInputStream(data);
    ObjectInputStream is = new ObjectInputStream(in);
    return is.readObject();
  }


  @Override
  public Object get(String key) throws RemoteException {
    ChordSession session = ObjectStoreService.getChordSession();
    ChordID<String> chordKey = new ChordID<>(key);
    ChordID<InetAddress> responsibleNodeID = session.getResponsibleNodeID(chordKey);
    ObjectStoreOperations responsibleStore = StoreRMIUtils.getRemoteObjectStore(responsibleNodeID.getKey());
    Object value = null;
    try {
      byte[] val = responsibleStore.getObject(chordKey);
      if (val == null) {
        logger.error("Key " + key + " not found on " + session.getChordNodeID());
      } else {
        value = deserialize(val);
      }
    } catch (Exception e) {
      logger.error(e);
    }
    return value;
  }


  @Override
  public List<Object> get(String key, long fromtimestamp, long totimestamp) throws RemoteException {
    ChordSession session = ObjectStoreService.getChordSession();
    ChordID<String> chordKey = new ChordID<>(key);
    ChordID<InetAddress> responsibleNodeID = session.getResponsibleNodeID(chordKey);
    ObjectStoreOperations responsibleStore = StoreRMIUtils.getRemoteObjectStore(responsibleNodeID.getKey());
    List<Object> value = null;
    try {
      List<byte[]> val = responsibleStore.getObject(chordKey, fromtimestamp, totimestamp);
      if (val == null) {
        logger.error("Key " + key + " not found on " + session.getChordNodeID());
      } else {
        logger.info("Received " + val.size() + " values for multi-read");
        value = new ArrayList<>();
        for (byte[] b : val) {
          logger.info(new String(b));
          value.add(deserialize(b));
        }
      }
    } catch (Exception e) {
      logger.error(e);
    }
    return value;
  }


  @Override
  public Object get(String key, long fromtimestamp) throws RemoteException {
    ChordSession session = ObjectStoreService.getChordSession();
    ChordID<String> chordKey = new ChordID<>(key);
    ChordID<InetAddress> responsibleNodeID = session.getResponsibleNodeID(chordKey);
    ObjectStoreOperations responsibleStore = StoreRMIUtils.getRemoteObjectStore(responsibleNodeID.getKey());
    Object value = null;
    try {
      byte[] val = responsibleStore.getObject(chordKey, fromtimestamp);
      if (val == null) {
        logger.error("Key " + key + " not found on " + session.getChordNodeID());
      } else {
        value = deserialize(val);
      }
    } catch (Exception e) {
      logger.error(e);
    }
    return value;
  }

  @Override
  public void put(String key, Object value) throws RemoteException {
    ChordSession session = ObjectStoreService.getChordSession();
    ChordID<String> chordKey = new ChordID<>(key);
    ChordID<InetAddress> responsibleNodeID = session.getResponsibleNodeID(chordKey);
    ObjectStoreOperations responsibleStore = StoreRMIUtils.getRemoteObjectStore(responsibleNodeID.getKey());

    /* Serialize the value */
    try {
      responsibleStore.putObject(chordKey, serialize(value));
    } catch (Exception e) {
      logger.error(e);
    }
  }

  @Override
  public void delete(String key) throws RemoteException {
    ChordSession session = ObjectStoreService.getChordSession();
    ChordID<String> chordKey = new ChordID<>(key);
    ChordID<InetAddress> responsibleNodeID = session.getResponsibleNodeID(chordKey);
    ObjectStoreOperations responsibleStore = StoreRMIUtils.getRemoteObjectStore(responsibleNodeID.getKey());
    responsibleStore.delete(chordKey);
  }

  @Override
  public List<KeyMetadata> keySet() throws RemoteException {
    return ObjectStoreService.getStore().keySet();
  }

//  @Override
//  public HashMap<String, DataContainer> dumpStore() throws RemoteException {
//    return ObjectStoreService.getStore().dumpStore();
//  }

}
