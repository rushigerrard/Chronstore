package edu.ncsu.store.persistence;

import edu.ncsu.store.KeyMetadata;
import edu.ncsu.store.LocalStorage;
import edu.ncsu.store.StoreConfig;
import edu.ncsu.store.utils.Pair;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ImmutableStore implements LocalStorage {

    /* Keep all loggers transient so that they are not passed over RMI call */
    private final transient static Logger logger = Logger.getLogger(ImmutableStore.class);

    /* Directory path where all index files and data files are stored */
    private String metaDirPath = StoreConfig.META_DIR;
    private String dataDirPath = StoreConfig.DATA_DIR;

    File indexDir = new File(metaDirPath);
    File dataDir = new File(dataDirPath);
    String metadataFilePath = metaDirPath + "metadata";

    /* A cache for storing the offset (in data file) at which lateset value is written
    * when this cache hit occurs we don't have to read index file for finding out offset
    * This will save a lot of time spend in reading and serializing index file*/
    ConcurrentHashMap<String, Pair<Integer, Integer>> offsetCache =
            new ConcurrentHashMap<>(1000, 0.9f, 2);


    /* Whenever multiple updates happen to same key our index file is written with new values.
     * we need to make sure that we don't allow multiple threads to write simultaneously. For this
     * write operations on index file of the same key will be synchronized. Moreover, each write
     * operation will write the new data to a different file and will then rename that file in
     * an atomic operation (Note: Atomic rename is supported on Linux, no guarantee that it will
     * work correctly on windows). Also, our readers can keep reading old file in between, but once
     * rename is done they will start seeing the new values.
     * To handle this we maintain a ReadWrite lock for each key in a ConcurrentHashMap, if that
     * value is present in the map, then it means some other thread is currently writing to the
     * file. Once the writing is done this value will be removed from hashMap, the waiting thread
     * (Which was in busy-wait while loop) will now see that key does not exist in hashmap anymore
     * and hence will go in.
     * */
    ConcurrentHashMap<String, Semaphore> fileAccessLocks =
            new ConcurrentHashMap<>(1000, 0.9f, 2);

    /* A separate lock is used for modifying metadata files */
    Semaphore metadataLock = new Semaphore(1);


    public ImmutableStore() throws Exception {
        // Create the data directories if they don't exist
        boolean res = true;
        File metadataFile = new File(metadataFilePath);
        if (!indexDir.exists()) {
            res &= indexDir.mkdir();
        }
        if (!dataDir.exists()) {
            res &= dataDir.mkdir();
        }
        if (!metadataFile.exists()) {
            metadataFile.createNewFile();
            // For metadata list, write empty list to file
            write(serialize(new HashSet<KeyMetadata>()), metadataFilePath, false);
        }
        if (!res) {
            throw new Exception("Data directories can not be created");
        }
    }

    /* helper methods */

    /**
     * To avoid creating too many files in a single directory, all index and data
     * files will be created inside a directory decided by the first two and last
     * two letters of the key. If the key length is less than 2 (i.e = 1), then we append an
     * extra character '_' at the end of of the key and then take those  two characters
     *
     * @param key
     * @return
     */
    private File indexFileFor(String key) {
        String firstTwoChar, lastTwoChar;
        if (key.length() < 2) {
            firstTwoChar = key + "_";
            lastTwoChar = key + "_";
        } else {
            firstTwoChar = key.substring(0, 2);
            lastTwoChar = key.substring(key.length() - 2, key.length());
        }
        String path = metaDirPath + firstTwoChar + File.separator + lastTwoChar +
                File.separator + key.toString() + ".index";
        return new File(path);
    }

    private File dataFilefor(String key) {
        String firstTwoChar, lastTwoChar;
        if (key.length() < 2) {
            firstTwoChar = key + "_";
            lastTwoChar = key + "_";
        } else {
            firstTwoChar = key.substring(0, 2);
            lastTwoChar = key.substring(key.length() - 2, key.length());
        }
        String path = dataDirPath + firstTwoChar + File.separator + lastTwoChar
                + File.separator + key.toString() + ".data";
        return new File(path);
    }

    /**
     * Creates a temporary storage files for given key. This essentially means creating the
     * index file and data file for that key with .tmp extension. This is because
     * when multiple thread try to read a file while one thread is creating it we don't
     * want any thread to read empty file
     *
     * @param key
     * @return
     */
    private boolean createStorageForKey(String key) {
        // if key index and data file is not already present then create it
        File indexFile = indexFileFor(key);
        File dataFile = dataFilefor(key);

        if (!dataFile.exists() && !indexFile.exists()) {
            try {
            /* Data file can not exist without a index file
             If index file is not found it means data file also
             has to be created. */
            /* Since we put all data and indexes in nested
            directories, we will have to create those too */
                logger.debug("New data files created for " + key);
                indexFile.getParentFile().mkdirs();
                indexFile.createNewFile();
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Simply renames the given file name from <filename>.tmp to <filename>
     *
     * @param tempFilePath
     */
    private void finalizeStorageFile(String tempFilePath, String newName) {
        try {
            Files.move(Paths.get(tempFilePath), Paths.get(newName),
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Renames to temporary storage created for the key to proper named copy
     *
     * @param key
     */
    private void finalizeStorageForKey(String key) {
        String tmpIndexFilePath = indexFileFor(key).getAbsolutePath() + ".tmp";
        String tmpDataFilePath = dataFilefor(key).getAbsolutePath() + ".tmp";
        String actualIndexFilePath = indexFileFor(key).getAbsolutePath();
        String actualDataFilePath = dataFilefor(key).getAbsolutePath();

        finalizeStorageFile(tmpDataFilePath, actualDataFilePath);
        finalizeStorageFile(tmpIndexFilePath, actualIndexFilePath);
    }

    /**
     * Reads the data from given file at given offset. Total number of bytes read will
     * be less than equal to len
     *
     * @param buffer   The buffer in which data should be read
     * @param filePath Path of the file
     * @param offset   offset at which data should be read
     * @param len      number of bytes that should be read
     * @return Returns the number of bytes read if successful, -1 otherwise
     */
    private int read(byte buffer[], String filePath, int offset, int len) {
        try (RandomAccessFile rafile = new RandomAccessFile(filePath, "r")) {
            rafile.seek(offset);
            rafile.read(buffer, 0, buffer.length);
            return buffer.length;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * writes the given object into given file by appending it
     * and returns the amount of bytes written. If boolean flag is set to
     * false then existing data will be overwritten by truncating the file.
     *
     * @param buffer   the byte buffer which should be written to the disk
     * @param filePath path of the file to which data should be written
     * @param append   If set to true then data will be appended to the file,
     *                 If set to false then file will be truncated first and then
     *                 data will be written to the file.
     * @return Returns number of bytes written if successful, -1 otherwise.
     */
    private int write(byte[] buffer, String filePath,
                      boolean append) {
        try (FileOutputStream fs = new FileOutputStream(filePath, append)) {
            if (!append) {
                // truncate the file
                fs.getChannel().truncate(0);
            }
            // write the data to file
            fs.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return buffer.length;
    }

    /**
     * serializes the given object stored and returns the
     * byte buffer of serialized data.
     *
     * @param obj Object to be serialied
     * @return Returns the byte buffer of serialized data
     */
    private byte[] serialize(Serializable obj) {
        // convert serializable object data into byte array.
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(obj);
            byte[] dataBytes = baos.toByteArray();
            return dataBytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserializes the object stored at given file and returns the
     * deserialized object.
     *
     * @param buffer the serialized data
     * @param c      Class of the object
     * @return
     */
    private <T> T deserialize(byte buffer[], Class<T> c) {
        Object o = null; // deserialized object;
        try {
            if (buffer != null && buffer.length > 0) {
                ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                ObjectInputStream ois = new ObjectInputStream(bais);
                o = ois.readObject();
                ois.close();
            }
            return c.cast(o);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private BPlusTree<Long, Integer> readIndexes(String filePath) {
        File indexFile = new File(filePath);
        byte buf[] = new byte[(int) indexFile.length()];
        int rlen = read(buf, filePath, 0, (int) indexFile.length());
        if (rlen == -1) {
            logger.error("Error while reading index file");
            return null;
        }
        return (BPlusTree<Long, Integer>) deserialize(buf, BPlusTree.class);
    }

    private HashSet<KeyMetadata> readMetadata() {
        File metadataFile = new File(metadataFilePath);
        byte[] serializedMetadata = new byte[(int) metadataFile.length()];
        read(serializedMetadata, metadataFile.getPath(), 0, (int) metadataFile.length());
        return deserialize(serializedMetadata, HashSet.class);
    }

    @Override
    public boolean containsKey(String key) {
        if (key == null || key.isEmpty())
            return false;
        HashSet<KeyMetadata> mSet = readMetadata();
        for (KeyMetadata km : mSet) {
            if (km.getKey().getKey().equals(key))
                return true;
        }
        return false;
    }

    @Override
    public KeyMetadata getMetadata(String key) {
        HashSet<KeyMetadata> mSet = readMetadata();
        for (KeyMetadata km : mSet) {
            if (km.getKey().getKey().equals(key))
                return km;
        }
        return null;
    }


    /**
     * This PUT API has only been added for testing purposes, hence it is not made public
     *
     * @param km        The keymetadata
     * @param value     value out the key
     * @return
     */
    boolean put(KeyMetadata km, byte[] value, long timestamp) {
        // First persist the data and indexes
        boolean success = false;
        if (km == null || value == null)
            throw new NullPointerException("Key or value can not be null!");

        if (put(km.getKey().getKey(), value, timestamp)) {
            // Write to data file is complete now add this into metadata list
            HashSet<KeyMetadata> mSet = readMetadata();
            if (!mSet.contains(km)) {
                mSet.add(km);
                // write back
                write(serialize(mSet), metadataFilePath, false);
            }
            success = true;
        }
        return success;
   }
    @Override
    public boolean put(KeyMetadata km, byte[] value) {
        // First persist the data and indexes
        boolean success = false;
        if (km == null || value == null)
            throw new NullPointerException("Key or value can not be null!");
        String indexFilePath, dataFilePath;
        try {
            indexFilePath = indexFileFor(km.getKey().getKey()).getCanonicalPath();
            dataFilePath = dataFilefor(km.getKey().getKey()).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        /* Get locks on both index file and data files, this is the first time
         * locks will be created for these files, all other operations should
         * directly take locks */
        fileAccessLocks.putIfAbsent(dataFilePath, new Semaphore(1));
        fileAccessLocks.putIfAbsent(indexFilePath, new Semaphore(1));

        try {
            fileAccessLocks.get(indexFilePath).acquire();
            fileAccessLocks.get(dataFilePath).acquire();
            if (put(km.getKey().getKey(), value, System.currentTimeMillis())) {
                // Write to data file is complete now add this into metadata list
                metadataLock.acquire();
                HashSet<KeyMetadata> mSet = readMetadata();
                if (!mSet.contains(km)) {
                    mSet.add(km);
                    // write back
                    write(serialize(mSet), metadataFilePath + ".tmp", false);
                    finalizeStorageFile(metadataFilePath + ".tmp", metadataFilePath);
                }
                metadataLock.release();
                success = true;
            }
            fileAccessLocks.get(dataFilePath).release();
            fileAccessLocks.get(indexFilePath).release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            success = false;
        }


        return success;
    }


    /* The B+Tree is essentially going to store a Long value for key, and
    an Integer value for the value. The Long value is going to be a timestamp of
    when the current value was added into the system and Integer value is going to
    be the offset of the actual storage location of  that value. */
    private boolean put(String key, byte[] value, long timestamp) {
        long before, after;
        BPlusTree<Long, Integer> indexTree;
        File dataFile = dataFilefor(key);
        File indexFile = indexFileFor(key);
        String indexFilePath = null, dataFilePath = null;

        try {
            indexFilePath = indexFile.getCanonicalPath();
            dataFilePath = dataFile.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        logger.debug("Data file " + dataFilePath + " index file " + indexFilePath);

        // we have to read index from file, but if this is the first
        // time inserting this key then we will have to create corresponding
        // files
        if (!indexFile.exists()) {
            createStorageForKey(key);
            indexTree = new BPlusTree<>();
        } else {
            // file was present - use Java serialization to read it.
            before = System.currentTimeMillis();
            indexTree = readIndexes(indexFile.getPath());
            after = System.currentTimeMillis();
            Profiler.logTimings(Profiler.Event.PUT_INDEX_READ, before, after);
        }

        // Now we have the index tree, write the object to the temporary dataFile
        // and then insert its index into B+Tree.
        int wlen = 0;
        if (value != null) {
            before = System.currentTimeMillis();
            wlen = write(value, dataFilePath, true);
            after = System.currentTimeMillis();
            Profiler.logTimings(Profiler.Event.PUT_DATA_WRITE, before, after);
            if (wlen == -1) {
                logger.error("Unable to write data");
                return false;
            }
        }
        // offset where this object was written is currently stored as the
        // last leaf of index tree. Get the new offset for next object
        // by adding length of currently written object into previous object
        // However, the get method returns pair of last two offsets,
        // In this pair, pair.first has the ending value and pair.second has
        // the start value.
        // we just need the ending value (pair.first) of that
        Pair<Integer, Integer> prevOffsetPair;
        if (offsetCache.containsKey(key)) {
            prevOffsetPair = offsetCache.get(key);
        } else {
            prevOffsetPair = indexTree.get();
        }
        int prevOffset;
        if (prevOffsetPair == null) {
            prevOffset = 0;
        } else {
            prevOffset = prevOffsetPair.getFirst();
        }
        int offset = prevOffset + wlen;
        logger.info("{" + key + "}: Written new value between " + prevOffset + " - " + offset);
        indexTree.insert(timestamp, offset);
        // write back the index tree
        before = System.currentTimeMillis();
        wlen = write(serialize(indexTree), indexFilePath + ".tmp", false);
        after = System.currentTimeMillis();
        Profiler.logTimings(Profiler.Event.PUT_INDEX_WRITE, before, after);
        if (wlen == -1) {
            logger.error("Error while writing index tree");
            return false;
        }
        finalizeStorageFile(indexFilePath + ".tmp", indexFilePath);

        // Also put this offset pair in offset cache - don't put this before finalizing storage
        // otherwise other thread might read offset from cache but won't find data.
        offsetCache.put(key, new Pair<>(offset, prevOffset));

        return true;
    }

    private byte[] readBetween(String filePath, Pair<Integer, Integer> startEndPair) {
        int length = startEndPair.getFirst() - startEndPair.getSecond();
        byte[] buf = new byte[length];
        int rlen = read(buf, filePath, startEndPair.getSecond(), length);
        if (rlen == -1) {
            //TODO add logger
            logger.error("Error while reading from datafile");
            return null;
        }
        return buf;
    }

    /**
     * Checks the returned pair for corner cases. If pair is null then
     * sets both starting and ending point to 0 (so that nothing is read)
     * If pair.getSecond() == null then sets the pair.value = 0. Having a
     * value as null means there is no previous element. Which means read should
     * start at offset 0.
     * Having a non-null value but null key is an Error.
     *
     * @param p
     * @return
     */
    private Pair<Integer, Integer> errorCheck(Pair<Integer, Integer> p) {
        if (p == null)
            p = new Pair<>(0, 0);
        else if (p.getSecond() == null)
            p.setSecond(0);
        return p;
    }

    @Override
    public byte[] get(String key, Long timestamp) {
        if (!containsKey(key))
            return null;

        byte[] retVal = null;
        BPlusTree<Long, Integer> indexTree;
        String dataFilePath, indexFilePath;
        try {
            dataFilePath = dataFilefor(key).getCanonicalPath();
            indexFilePath = indexFileFor(key).getCanonicalPath();
            indexTree = readIndexes(indexFilePath);
            Pair<Integer, Integer> p = indexTree.get(timestamp);
            p = errorCheck(p);
            // returned pair object has key as the offset at timestamp and value
            // as the offset before timestamp.
            retVal = readBetween(dataFilePath, p);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retVal;
    }


    @Override
    public byte[] get(String key) {
        if (!containsKey(key))
            return null;

        long before, after;
        Pair<Integer, Integer> p;
        byte[] retVal = null;
        try {
            String dataFilePath = dataFilefor(key).getCanonicalPath();
            String indexFilePath = indexFileFor(key).getCanonicalPath();
        /* This is a call for latest value, see if its offset is available in cache
        and use that to avoid read of index file */
            if (offsetCache.containsKey(key)) {
                p = offsetCache.get(key);
            } else {
                BPlusTree<Long, Integer> indexTree;
                before = System.currentTimeMillis();
                indexTree = readIndexes(indexFilePath);
                after = System.currentTimeMillis();
                Profiler.logTimings(Profiler.Event.GET_INDEX_READ, before, after);
                p = indexTree.get();
            }
            p = errorCheck(p);
            // returned pair object has key as the offset at timestamp and value
            // as the offset before timestamp.
            before = System.currentTimeMillis();
            retVal = readBetween(dataFilePath, p);
            after = System.currentTimeMillis();
            Profiler.logTimings(Profiler.Event.GET_DATA_READ, before, after);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return retVal;
    }

    @Override
    public List<byte[]> get(String key, Long fromTime, Long toTime) {
        if (!containsKey(key))
            return null;

        BPlusTree<Long, Integer> indexTree;
        List<byte[]> result = Collections.emptyList();
        try {
            String dataFilePath = dataFilefor(key).getCanonicalPath();
            String indexFilePath = indexFileFor(key).getCanonicalPath();
            indexTree = readIndexes(indexFilePath);
            List<Pair<Integer, Integer>> pList = indexTree.get(fromTime, toTime);
            result = new ArrayList<>();
            // If no key was found within this time interval then this list will be empty
            for (Pair<Integer, Integer> p : pList) {
                errorCheck(p);
                logger.info("{" + key +"}:" + "Reading between " + p.getSecond() + " and " + p.getFirst());
                result.add(readBetween(dataFilePath, p));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * Since this is an immutable database, we do not delete any key
     * Instead we simply put a new value for this key as string null.
     * Whenever someone does a get and if the value is string null
     * we return null to show that value didn't exist at that time.
     * If key is not present already and if delete is called on such key
     * then we don't do anything. Instead we simply return.
     *
     * @param key
     * @return
     */
    // TODO decide what to do when key is not put yet but still a delete occurs
    @Override
    public boolean delete(String key) {
        if (containsKey(key)) {
            put(key, null, System.currentTimeMillis());
        }
        return true;
    }

    @Override
    public List<KeyMetadata> keySet() {
        return new ArrayList<>(readMetadata());
    }

}
