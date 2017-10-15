package edu.ncsu.store.persistence;


import java.util.ArrayList;
import java.util.List;

public class LeafNode<K extends Comparable<K>, T> extends Node<K, T> {
    /* The B-tree is just a index it doesn't store actual values, the actual
    values are stored on a secondary storage medium inside a file corresponding to this key.
    The offset of each value is stored in this index. The valueOffsets list maintains the
    ending offset of each value. i.e the very first value is obviously going to have starting offset
    zero and the ending offset of that value will be stored in this arraylist of that LeafNode.
    So, if we need to find out the starting and ending point of a particular value then we need to
    search for that value first then find out the offset of that value and then find out the offset
    of previous value.
     */
    protected ArrayList<T> valueOffsets;
    protected LeafNode<K, T> nextLeaf;
    protected LeafNode<K, T> previousLeaf;

    public LeafNode(K firstKey, T firstValue) {
        isLeafNode = true;
        keys = new ArrayList<K>();
        valueOffsets = new ArrayList<T>();
        keys.add(firstKey);
        valueOffsets.add(firstValue);
    }

    public LeafNode(List<K> newKeys, List<T> newValues) {
        isLeafNode = true;
        keys = new ArrayList<K>(newKeys);
        valueOffsets = new ArrayList<T>(newValues);

    }

    /**
     * returns the values at given index
     * @param index
     * @return
     */
    public T getValue(int index) {
        if (index >= 0 && index < valueOffsets.size())
            return valueOffsets.get(index);
        return null;
    }

    /**
     * returns the First values stored at this node
     * @return
     */
    public T getFirstValue() {
        if (valueOffsets.size() > 0)
            return valueOffsets.get(0);
        return null;
    }

    /**
     * returns the Last values stored at this node
     * @return
     */
    public T getLastValue() {
        if (valueOffsets.size() > 0)
            return valueOffsets.get(valueOffsets.size() - 1);
        return null;
    }

    /**
     * insert key/value into the leaf node
     *
     * @param key
     * @param value
     */
    public void insert(K key, T value) {

        if (key.compareTo(keys.get(keys.size() - 1)) > 0) {
            keys.add(key);
            valueOffsets.add(value);
        }
    }

}

