package edu.ncsu.chord;

import java.lang.reflect.Array;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by amit on 23/2/17.
 */
class ChordConfig {

  /* Depending on CHORD_ID_MAX_BITS in each ChordID will be calculated. This is also used in finger table */
  static int CHORD_ID_MAX_BITS = 32;

  /* Number of maximum entries to keep in successor list */
  static int SUCCESSOR_LIST_MAX_SIZE = 3;

  /* ArrayList of IPs of all bootstrap nodes */
  static ArrayList<InetAddress> bootstrapNodes;

  static {
    // Read environment variables for finding out bootstrap nodes
    // An environment variable named CHRON_BOOTSTRAP_NODELIST will have
    // a comma separated list of bootstrap node IP
    bootstrapNodes = new ArrayList<>();
    String nodeList = System.getenv("CHRON_BOOTSTRAP_NODELIST");
    if (nodeList != null && !nodeList.isEmpty()) {
      String ipArray[] = nodeList.split(",");
      try {
        for (String s : ipArray) {
          if (s != null && !s.isEmpty())
            bootstrapNodes.add(InetAddress.getByName(s));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /* RMI Registry Port */
  static int RMI_REGISTRY_PORT = 1099;

  /* Network interface to be used for communication */
  static String NetworkInterface = "eth1";

  /* Seconds to wait before stabilization process starts */
  static int STABILIZER_INITIAL_DELAY = 2;

  /* Seconds after which stabilizer function should be called again */
  static int STABILIZER_PERIOD = 2;
}
