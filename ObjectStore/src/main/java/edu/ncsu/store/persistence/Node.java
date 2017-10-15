package edu.ncsu.store.persistence;

import java.io.Serializable;
import java.util.ArrayList;


public class Node<K extends Comparable<K>, T> implements Serializable {
    protected boolean isLeafNode;
    protected ArrayList<K> keys;

    /**
     * Todo : liveNode pointing to latest node
     */
    protected LeafNode liveNode;

    public boolean isOverflowed() {
        return keys.size() > 2 * BPlusTree.D;
    }

}

