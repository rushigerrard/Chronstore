package edu.ncsu.store.persistence;

import edu.ncsu.chord.ChordID;
import edu.ncsu.store.KeyMetadata;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ImmutableStoreStressTest {

    private static final int MAX_KEYS = 50;

    private static final int BUFFER_MAX_SIZE = 512;

    private static final int THREAD_ITERATIONS = 60;

    private static final int THREAD_COUNT = 10;

    /*System temporary directory*/
    private final static String TMP_DIR = System.getProperty("java.io.tmpdir");
    
    private static final String randomDatafile = TMP_DIR + File.separator + "random.txt";

    byte[] randomData;

    /*
     * Each entry in key-value map will hold the list
     * of all byte[] arrays which are assigned to this key till now
     * Note that these arrays will always be taken from the dataList
     * The key itself will be generated by using a UUID.
     * */
    Map<String, List<byte[]>> keyValueMap = new HashMap<>();

    /* For the values we read a big chunk of text from the file called
      'random.txt' and then pick up random sized text strings from that text as
       values of our keys. */
    List<byte[]> dataList = new ArrayList<>(1001);

    /* List of read only keys */
    List<String> keyList = new ArrayList<>();

    Random random = new Random();

    /* Instance of immutable store to test upon */
    ImmutableStore imstore;

    private void loadRandomText() {
        try {
            randomData = Files.readAllBytes(Paths.get(randomDatafile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates a random byte array of given length. The content of this
     * byte array is taken from the random text segment
     * @param len
     * @return
     */
    private byte[] generateRandomData(int len) {
        byte[] data = new byte[len];
        int currLen = 0;
        while (currLen < len) {
            int offset = random.nextInt(randomData.length - 1);
            int size = Math.min(randomData.length - offset - 1, len - currLen);
            System.arraycopy(randomData, offset, data, currLen, size);
            currLen += size;
        }
        return data;
    }


    public ImmutableStoreStressTest() {
        // Generate random data which can be added into the system.
        loadRandomText();

        // Generate random byte arrays to be used as values
        for (int i = 0; i < MAX_KEYS; i++) {
            int len = random.nextInt(BUFFER_MAX_SIZE) + 32;
            dataList.add(generateRandomData(len));
        }

        // Generate random keys
        while (keyValueMap.size() < MAX_KEYS) {
            String key = UUID.randomUUID().toString().replace("-", "");
            if (!keyValueMap.containsKey(key)) {
                keyValueMap.put(key, Collections.synchronizedList(new ArrayList<byte[]>()));
                keyList.add(key);
            }
        }

        try {
            imstore = new ImmutableStore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    AtomicLong getTime = new AtomicLong();
    AtomicLong putTime = new AtomicLong();

    public void stressTest() {

        Runnable testerThread = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < THREAD_ITERATIONS; i++) {
                    int index = random.nextInt(MAX_KEYS - 1);
                    String key = keyList.get(index);
                    List<byte[]> valueList = keyValueMap.get(key);
                    index = random.nextInt(MAX_KEYS - 1);
                    KeyMetadata km = new KeyMetadata(new ChordID<>(key));
                    long before = System.currentTimeMillis();
                    if(!imstore.put(km, dataList.get(index))) {
                        System.out.println("Key put failed!");
                        continue;
                    }
                    long after = System.currentTimeMillis();
                    putTime.addAndGet((after - before));
                    valueList.add(dataList.get(index));
                }
            }
        };

        ExecutorService es = Executors.newCachedThreadPool();
        for (int i = 0; i < THREAD_COUNT; i++) {
            es.execute(testerThread);
        }
        es.shutdown();
        boolean done = false;
        while (!done) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            done = es.isTerminated();
        }


    }

    private void verify(long before, long after) {
        // First check if all keys are available
        List<KeyMetadata> l = imstore.keySet();
//        Assert.assertEquals("Number of keys should be exactly equal",
//                l.size(), keyList.size());
        for (Map.Entry<String, List<byte[]>> e : keyValueMap.entrySet()) {
            if (e.getValue().size() > 0) {
                KeyMetadata km = new KeyMetadata(new ChordID<>(e.getKey()));
                Assert.assertTrue("Key " + e.getKey() + " Not found!",
                        l.contains(km));
                List<byte[]> allValues = e.getValue();
                long start = System.currentTimeMillis();
                List<byte[]> actualValues = imstore.get(km.getKey().getKey(), before, after);
                long end = System.currentTimeMillis();
                getTime.addAndGet((end - start));
                Assert.assertEquals("Number of values returned for a key don't match",
                        allValues.size(), actualValues.size());
                for (int i = 0; i < allValues.size(); i++) {
                    if (!Arrays.equals(actualValues.get(i), allValues.get(i)))
                        System.out.println("Values don't match\n "+
                        " expected: [" + new String(allValues.get(i)) + "]\n" +
                        " actual: [" + new String(actualValues.get(i)) + "]\n");
                }
//                Assert.assertTrue("History doesnt match for key " + e.getKey()
//                                + " actual size " + actualValues.size() + " expected size " + allValues.size(),
//                        allValues.containsAll(actualValues));
            }
        }

    }

    private static void delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        f.delete();
    }

    public static void main(String args[]) {
        // clean old data first
        delete(new File(TMP_DIR + File.separator +"data"));
        delete(new File(TMP_DIR + File.separator + "indexes"));
        long before = System.currentTimeMillis();
        ImmutableStoreStressTest isst = new ImmutableStoreStressTest();
        isst.stressTest();
        long after = System.currentTimeMillis();
        isst.verify(before, after);
        System.out.println("Average time for put key: " +
                (isst.putTime.doubleValue() / (THREAD_ITERATIONS * THREAD_COUNT)));
        System.out.println("Average time for get key: " +
                (isst.getTime.doubleValue() / (THREAD_ITERATIONS * THREAD_COUNT)));
    }


}