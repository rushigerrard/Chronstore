package edu.ncsu.store;

import java.io.File;

/**
 * Created by amit on 1/4/17.
 */
public class StoreConfig {

  /* RMI Registry Port */
  static int RMI_REGISTRY_PORT = 1099;

  /* Number of replicas to maintain */
  public static int REPLICATION_COUNT = 2;

  /* RMI Call timeout - Seconds to wait before call is considered as failed */
  static int RMI_TIMEOUT = 1;

  /*System temporary directory*/
  private final static String TMP_DIR = System.getProperty("java.io.tmpdir");
  
  /* metadata directory paths - this directory will store indexes and metadata */
  public final static String META_DIR = TMP_DIR + File.separator + "indexes" + File.separator;

  /* Data directory path - this directory will store actual data */
  public final static String DATA_DIR = TMP_DIR + File.separator + "data" + File.separator;

}
