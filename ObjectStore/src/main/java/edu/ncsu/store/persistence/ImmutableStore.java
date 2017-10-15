package edu.ncsu.store.persistence;

import edu.ncsu.store.DataContainer;
import edu.ncsu.store.KeyMetadata;
import edu.ncsu.store.LocalStorage;

import java.util.HashMap;
import java.util.List;

public class ImmutableStore implements LocalStorage {

    HashMap<String, byte[]> data;

    public ImmutableStore() {
        data = new HashMap<>();
    }

    @Override
    public byte[] get(String key) {
        return data.get(key);
    }

    @Override
    public byte[] get(String key, Long timestamp) {
        return new byte[0];
    }

    @Override
    public List<byte[]> get(String key, Long fromTime, Long toTime) {
        return null;
    }

    @Override
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    @Override
    public KeyMetadata getMetadata(String key) {
        return null;
    }

    @Override
    public boolean put(KeyMetadata km, byte[] value) {
        if (data.put(km.getKey().getKey(), value) != null)
            return true;
        return false;
    }

    @Override
    public boolean delete(String key) {
        return false;
    }

    @Override
    public List<KeyMetadata> keySet() {
        return null;
    }

//    @Override
//    public HashMap<String, DataContainer> dumpStorage() {
//        return null;
//    }
}
