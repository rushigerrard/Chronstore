package edu.ncsu.store.persistence;

import edu.ncsu.store.KeyMetadata;
import edu.ncsu.store.LocalStorage;
import edu.ncsu.store.StoreConfig;
import edu.ncsu.store.utils.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImmutableStore implements LocalStorage {
    //TODO: add unit & functional tests

    /* Directory path where all index files and data files are stored */
    private String metaDirPath = StoreConfig.META_DIR;
    private String dataDirPath = StoreConfig.DATA_DIR;

    /* This string is used to represent a deleted value */
    private static final byte[] NULLDATA = "null".getBytes();

    File indexDir = new File(metaDirPath);
    File dataDir = new File(dataDirPath);
    File metadataFile = new File(metaDirPath + "metadata");

    // HashMap<String, byte[]> data;

    public ImmutableStore() throws Exception {
        // Create the data directories if they don't exist
        boolean res = true;
        if (!indexDir.exists()) {
            res &= indexDir.mkdir();
        }
        if (!dataDir.exists()) {
            res &= dataDir.mkdir();
        }
        if (!metadataFile.exists()) {
            metadataFile.createNewFile();
            // For metadata list, write empty list to file
            write(serialize(new ArrayList<KeyMetadata>()), metadataFile.getPath(), false);
        }
        if (!res) {
            throw new Exception("Data directories can not be created");
        }
    }


    /* helper methods */
    private File indexFileFor(String key) {
        return new File(metaDirPath + key.toString() + ".index");
    }

    private File dataFilefor(String key) {
        return new File(dataDirPath + key.toString() + ".data");
    }

    /**
     * Creates storage files for given key. This essentially means creating the
     * index file and data file for that key.
     *
     * @param key
     * @return
     */
    private boolean createStorageForKey(String key) {
        // if key index and data file is not already present then create it
        File dataFile = indexFileFor(key);
        File indexFile = dataFilefor(key);
        if (!dataFile.exists() && !indexFile.exists()) {
            try {
            /* Data file can not exist without a index file
             If index file is not found it means data file also
             has to be created. */
                System.out.println("New data files created for " + key);
                indexFile.createNewFile();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
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
        try (FileOutputStream fs = new FileOutputStream(filePath, append)){
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
            System.out.println("Error while reading index file");
            return null;
        }
        return (BPlusTree<Long, Integer>) deserialize(buf, BPlusTree.class);
    }

    private ArrayList<KeyMetadata> readMetadata() {
        byte[] serializedMetadata = new byte[(int) metadataFile.length()];
        read(serializedMetadata, metadataFile.getPath(), 0, (int) metadataFile.length());
        return deserialize(serializedMetadata, ArrayList.class);
    }

    @Override
    public boolean containsKey(String key) {
        if (key == null || key.isEmpty())
            return false;
        ArrayList<KeyMetadata> mList = readMetadata();
        for (KeyMetadata km : mList) {
            if (km.getKey().getKey().equals(key))
                return true;
        }
        return false;
    }

    @Override
    public KeyMetadata getMetadata(String key) {
        ArrayList<KeyMetadata> mList = readMetadata();
        for (KeyMetadata km : mList) {
            if (km.getKey().getKey().equals(key))
                return km;
        }
        return null;
    }


    @Override
    public boolean put(KeyMetadata km, byte[] value) {
        // First persist the data and indexes
        boolean success = false;
        if (km == null || value == null)
            throw new NullPointerException("Key or value can not be null!");

        if (put(km.getKey().getKey(), value)) {
            // Write to data file is complete now add this into metadata list
            ArrayList<KeyMetadata> mList = readMetadata();
            if (!mList.contains(km)) {
                mList.add(km);
                // write back
                write(serialize(mList), metadataFile.getPath(), false);
            }
            success = true;
        }
        return success;
    }

    /* The B+Tree is essentially going to store a Long value for key, and
an Integer value for the value. The Long value is going to be a timestamp of
when the current value was added into the system and Integer value is going to
be the offset of the actual storage location of  that value. */
    private boolean put(String key, byte[] value) {
        BPlusTree<Long, Integer> indexTree;
        File dataFile = dataFilefor(key);
        File indexFile = indexFileFor(key);
        System.out.println("Data file " + dataFile.getPath() + " index file " + indexFile.getPath());
        // we have to read index from file, but if this is the first
        // time inserting this key then we will have to create corresponding
        // files
        if (!indexFile.exists()) {
            createStorageForKey(key.toString());
            indexTree = new BPlusTree<>();
        } else {
            // file was present - use Java serialization to read it.
            indexTree = readIndexes(indexFile.getPath());
        }

        // Now we have the index tree, write the object to the dataFile
        // and then insert its index into B+Tree.
        int wlen = 0;
        if (value != null) {
            wlen = write(value, dataFile.getPath(), true);
            if (wlen == -1) {
                System.out.println("Unable to write data");
                return false;
            }
        }
        // offset where this object was written is currently stored as the
        // last leaf of index tree. Get the new offset for next object
        // by adding length of currently written object into previous object
        // However, the get method returns pair of last two offsets, we just
        // need second one of that pair
        Pair<Integer, Integer> prevOffsetPair = indexTree.get();
        int prevOffset;
        if (prevOffsetPair == null) {
            prevOffset = 0;
        } else {
            prevOffset = prevOffsetPair.getKey();
        }
        int offset = prevOffset + wlen;
        System.out.println("Written new value between " + prevOffset + " - " + offset);
        indexTree.insert(System.currentTimeMillis(), offset);

        // write back the index tree
        wlen = write(serialize(indexTree), indexFile.getPath(), false);
        if (wlen == -1) {
            System.out.println("Error while writing index tree");
            return false;
        }
        return true;
    }

    private byte[] readBetween(String filePath, Pair<Integer, Integer> startEndPair) {
        int length = startEndPair.getKey() - startEndPair.getValue() ;
        byte[] buf = new byte[length];
        int rlen = read(buf, filePath, startEndPair.getValue(), length);
        if (rlen == -1) {
            //TODO add logger
            System.out.println("Error while reading from datafile");
            return null;
        }
        return buf;
    }

    /**
     * Checks the returned pair for corner cases. If pair is null then
     * sets both starting and ending point to 0 (so that nothing is read)
     * If pair.getValue() == null then sets the pair.value = 0. Having a
     * value as null means there is no previous element. Which means read should
     * start at offset 0.
     * Having a non-null value but null key is an Error.
     * @param p
     * @return
     */
    private Pair<Integer, Integer> errorCheck(Pair<Integer, Integer> p) {
        if (p == null)
            p = new Pair<>(0, 0);
        else if (p.getValue() == null)
            p.setValue(0);
        return p;
    }

    @Override
    public byte[] get(String key, Long timestamp) {
        BPlusTree<Long, Integer> indexTree;
        String dataFilePath = dataFilefor(key).getPath();
        String indexFilePath = indexFileFor(key).getPath();
        indexTree = readIndexes(indexFilePath);
        Pair<Integer, Integer> p = indexTree.get(timestamp);
        p = errorCheck(p);
        // returned pair object has key as the offset at timestamp and value
        // as the offset before timestamp.
        return readBetween(dataFilePath, p);
    }


    @Override
    public byte[] get(String key) {
        BPlusTree<Long, Integer> indexTree;
        String dataFilePath = dataFilefor(key).getPath();
        String indexFilePath = indexFileFor(key).getPath();
        indexTree = readIndexes(indexFilePath);
        Pair<Integer, Integer> p = indexTree.get();
        p = errorCheck(p);
        // returned pair object has key as the offset at timestamp and value
        // as the offset before timestamp.
        return readBetween(dataFilePath, p);
    }

    @Override
    public List<byte[]> get(String key, Long fromTime, Long toTime) {
        BPlusTree<Long, Integer> indexTree;
        String dataFilePath = dataFilefor(key).getPath();
        String indexFilePath = indexFileFor(key).getPath();
        indexTree = readIndexes(indexFilePath);
        List<Pair<Integer, Integer>> pList = indexTree.get(fromTime, toTime);
        List<byte[]> result = new ArrayList<>();
        // If no key was found within this time interval then this list will be empty
        for (Pair<Integer, Integer> p : pList) {
            errorCheck(p);
            System.out.println("Reading between " + p.getValue() + " and " + p.getKey());
            result.add(readBetween(dataFilePath, p));

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
     * @param key
     * @return
     */
    // TODO decide what to do when key is not put yet but still a delete occurs
    @Override
    public boolean delete(String key) {
        if (containsKey(key)) {
            put(key, null);
        }
        return true;
    }

    @Override
    public List<KeyMetadata> keySet() {
        return readMetadata();
    }

}
