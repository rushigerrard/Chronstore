package edu.ncsu.store.persistence;

import edu.ncsu.chord.ChordID;
import edu.ncsu.store.KeyMetadata;
import edu.ncsu.store.StoreConfig;
import edu.ncsu.store.utils.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

public class ImmutableStoreTest {

    ImmutableStore store;

    @Before
    public void init() {
        // Delete files created by previous tests
        delete(new File(StoreConfig.DATA_DIR));
        delete(new File(StoreConfig.META_DIR));
        try {
            store = new ImmutableStore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        f.delete();
    }

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


    @Test
    public void containsKey() throws Exception {
        store.put(new KeyMetadata(new ChordID<>("testkey0")), "testdata0".getBytes());
        store.put(new KeyMetadata(new ChordID<>("testkey1")), "testdata1".getBytes());
        store.put(new KeyMetadata(new ChordID<>("testkey2")), "testdata2".getBytes());
        store.put(new KeyMetadata(new ChordID<>("testkey3")), "testdata3".getBytes());
        Assert.assertTrue("Key should be found", store.containsKey("testkey0"));
        Assert.assertFalse("Key should NOT be found", store.containsKey("testkey10"));
        Assert.assertFalse("Key should NOT be found", store.containsKey("testdata1"));
        Assert.assertFalse("Key should NOT be found", store.containsKey(null));
    }

    @Test
    public void getMetadata() throws Exception {
        for (int i = 0; i < 10; i++) {
            String key = "testkey" + i;
            KeyMetadata km = new KeyMetadata(new ChordID<>(key));
            km.setReplicaNumber(i);
            store.put(km, "testdata".getBytes());
        }

        Assert.assertTrue("Metadata for non-existent key should be null",
                store.getMetadata("randomKey") == null);

        for (int i = 9; i >= 0; i--) {
            String key = "testkey" + i;
            KeyMetadata km = store.getMetadata(key);
            Assert.assertEquals("Key strings should match", km.getKey(), new ChordID<>(key));
            Assert.assertEquals("Replica numbers should match", km.getReplicaNumber(), i);
        }

    }

    @Test
    public void putMultiple() throws Exception {
        Long before = System.currentTimeMillis();
        String key = "testkey";
        KeyMetadata km = new KeyMetadata(new ChordID<>(key));
        ArrayList<Pair<String, Integer>> data = new ArrayList();
        data.add(new Pair<>("abc", 1));
        data.add(new Pair<>("xyz", 2));
        data.add(new Pair<>("pqr", 3));
        data.add(new Pair<>("lmn", 4));
        store.put(km, serialize(data));
        store.delete(key);
        store.put(km, "stringdata".getBytes());
        Thread.sleep(100);
        Long after = System.currentTimeMillis();
        List<byte[]> values = store.get(key, before, after);

        Assert.assertTrue(Arrays.equals(values.get(0), serialize(data)));
        Assert.assertTrue(Arrays.equals(values.get(1), "".getBytes()));
        Assert.assertTrue(Arrays.equals(values.get(2), "stringdata".getBytes()));

    }

    @Test
    public void putSingle() throws Exception {
        String key = "testkey";
        String data = "testdata";
        KeyMetadata km = new KeyMetadata(new ChordID<>(key));
        store.put(km, data.getBytes());
        Thread.sleep(100);
        byte[] retVal = store.get(key);
        //System.out.println("received: ["+new String(retVal)+"]");
        Assert.assertEquals("get value doesn't match with put value", data, new String(retVal));
    }

    @Test
    public void putMultiple2() {
        String key = "testkey";
        KeyMetadata km = new KeyMetadata(new ChordID<>(key));
        store.put(km, "1234".getBytes());
        store.put(km, "2345".getBytes());
        store.put(km, "3456".getBytes());
        store.put(km, "4567".getBytes());
        List<byte[]> values = store.get(key, (long) 1, System.currentTimeMillis() + 100);

        Assert.assertTrue(new String(values.get(0)).equals("1234"));
        Assert.assertTrue(new String(values.get(1)).equals("2345"));
        Assert.assertTrue(new String(values.get(2)).equals("3456"));
        Assert.assertTrue(new String(values.get(3)).equals("4567"));
    }


    //
//    @Test
//    public void get() throws Exception {
//    }
//
//    @Test
//    public void get1() throws Exception {
//    }
//
//    @Test
//    public void get2() throws Exception {
//    }
//
//    @Test
//    public void delete() throws Exception {
//    }
//
    @Test
    public void keySet() throws Exception {
        List<KeyMetadata> expectedList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String key = "testkey" + i;
            KeyMetadata km = new KeyMetadata(new ChordID<>(key));
            km.setReplicaNumber(i);
            store.put(km, "testdata".getBytes());
            expectedList.add(km);
        }
        List<KeyMetadata> actualList = store.keySet();
        Assert.assertTrue("Key and its metadata should match",
                actualList.containsAll(expectedList));
    }

}