package edu.ncsu.store.persistence;

import edu.ncsu.store.utils.Pair;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * BPlusTree Class Assumptions: 1. No duplicate keys inserted 2. Order D:
 * D<=number of keys in a node <=2*D 3. All keys are non-negative
 * TODO: Rename to BPlusTree
 */
public class BPlusTree<K extends Comparable<K>, T> implements Serializable {

    public Node<K,T> root;
    public static final int D = 16;

    /**
     * The get(K startTime, K endTime) returns all timestamps in the given range
     * inclusive of start and end time startTime < endTime
     *
     * @param startTime
     * @param endTime
     * @return
     */
    public List<Pair<T, T>> get(K startTime, K endTime) {

        List<Pair<T,T>> finalList = new LinkedList<>();
        LeafNode<K, T> endLeaf = (LeafNode<K, T>) treeSearch(root, endTime);

        // traverse from endTime to startTime (reverse introduces edge-cases
        // which can be avoided)
        int counter = 0;
        K currentTime = endTime;
        while(endLeaf != null){
            List<T> values = endLeaf.valueOffsets;
            List<K> keys = endLeaf.keys;

            for (int i = keys.size() - 1; i >= 0 &&
                    currentTime.compareTo(startTime) >= 0; i--) {

                // if timestamp in leafnode is less than or equal to endtime, add associated value to the finalList
                if (keys.get(i).compareTo(endTime) <= 0 &&
                        keys.get(i).compareTo(startTime) >= 0) {
                    finalList.add(0, new Pair<>(values.get(i), getPrevious(endLeaf, i)));
                }
                currentTime = keys.get(i);
            }
            endLeaf = endLeaf.previousLeaf;
        }
        return finalList;

    }


    /**
     * Given a reference to a LeafNode and and index of entry inside
     * that leaf Node returns the previous value of given index.
     * In simple cases this previous values is just i-1th value
     * but in some cases previous values might exist on another sibling
     * node, in a special case of very first values previous values
     * will be considered as 0.
     * @param leafNode
     * @param index
     * @return
     */
    private T getPrevious(LeafNode<K, T> leafNode, int index) {
        if (index == 0) {
            if (leafNode.previousLeaf == null) {
                return null;
            } else {
                return leafNode.previousLeaf.getLastValue();
            }
        } else {
            return leafNode.getValue(index - 1);
        }
    }


    /**
     * The get(K timestamp) method returns a Pair of the offset and length of
     * the value associated with a key at
     * that particular timestamp.
     *
     * @param timestamp
     * @return
     */
    public Pair<T, T> get(K timestamp) {
        // Return if empty tree or key
        if (timestamp == null || root == null) {
            return null;
        }
        // Look if the given timestamp is greater than the last inserted value
        // In-case it is, return the value from liveNode itself
        LeafNode<K, T> leaf = root.liveNode;
        if (leaf != null && leaf.keys.size() != 0 &&
                leaf.keys.get(leaf.keys.size() - 1).compareTo(timestamp) < 0) {
            return new Pair<>(leaf.getLastValue(),
                    getPrevious(leaf, leaf.valueOffsets.size() - 1));
        }

        // Look for leaf node that key is pointing to
        leaf = (LeafNode<K, T>) treeSearch(root, timestamp);

        // Look for value in the leaf
        for (int i = 0; i < leaf.keys.size(); i++) {
            if (timestamp.compareTo(leaf.keys.get(i)) == 0) {
                return new Pair<>(leaf.getValue(i), getPrevious(leaf, i));
            }
            // Find the value associated with the immediate higher timestamp
            else if (timestamp.compareTo(leaf.keys.get(i)) < 0) {
                if (i != 0) { // return the value associated with just previous
                    // timestamp
                    return new Pair<>(leaf.valueOffsets.get(i - 1),
                            getPrevious(leaf, i - 1));

                } else {
                    // the previous timestamp could sometimes reside on a
                    // previous leaf
                    // if previousLeaf is null, no value existed at that
                    // timestamp
                    if (leaf.previousLeaf != null && leaf.previousLeaf.valueOffsets.size() > 0) {
                        return new Pair<>(leaf.previousLeaf.getLastValue(),
                                getPrevious(leaf.previousLeaf, leaf.previousLeaf.valueOffsets.size() - 1));
                    } else {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * The get(int k) method returns a list of the last k valueOffsets associated with
     * a key.
     * NOT implemented properly! DON'T use.
     * @param k
     * @return
     */
    private List<T> get(int k) {

        List<T> finalList = new ArrayList<>();
        LeafNode<K, T> newLeaf = root.liveNode;
        int counter = 0;

        while(newLeaf != null && counter < k){
            List<T> values = newLeaf.valueOffsets;

            for (int i = values.size() - 1; i >= 0 && counter < k; i--, counter++) {
                finalList.add(values.get(i));
            }

            newLeaf = newLeaf.previousLeaf;
        }
        return finalList;
    }

    /**
     * The get() method returns the latest value associated with a key.
     *
     * @return value
     */
    public Pair<T, T> get() {
        if (root != null && root.liveNode != null && root.liveNode.valueOffsets.size() != 0)
            return new Pair<>(root.liveNode.getLastValue(),
                    getPrevious(root.liveNode, root.liveNode.valueOffsets.size() - 1));
        return null;
    }


    private Node<K, T> treeSearch(Node<K, T> node, K key) {
        if (node.isLeafNode) {
            return node;
        }
        // The node is index node
        else {
            IndexNode<K, T> index = (IndexNode<K, T>) node;

            // K < K1, return treeSearch(P0, K)
            if (key.compareTo(index.keys.get(0)) < 0) {
                return treeSearch((Node<K, T>) index.children.get(0), key);
            }
            // K >= Km, return treeSearch(Pm, K), m = #entries
            else if (key.compareTo(index.keys.get(node.keys.size() - 1)) >= 0) {
                return treeSearch((Node<K, T>) index.children.get(index.children.size() - 1), key);
            }
            // Find i such that Ki <= K < K(i+1), return treeSearch(Pi,K)
            else {
                // Linear searching
                for (int i = 0; i < index.keys.size() - 1; i++) {
                    if (key.compareTo(index.keys.get(i)) >= 0 && key.compareTo(index.keys.get(i + 1)) < 0) {
                        return treeSearch((Node<K, T>) index.children.get(i + 1), key);
                    }
                }
            }
            return null;
        }
    }

    /**
     * TODO Insert a key/value pair into the BPlusTree
     *
     * @param key
     * @param value
     */
    public void insert(K key, T value) {
        LeafNode<K,T> newLeaf = new LeafNode<K,T>(key, value);
        Entry<K, Node<K,T>> entry = new AbstractMap.SimpleEntry<K, Node<K,T>>(key, newLeaf);

        // Insert entry into subtree with root node pointer
        if(root == null || root.keys.size() == 0) {
            root = entry.getValue();
        }

        // newChildEntry null initially, and null on return unless child is split
        Entry<K, Node<K,T>> newChildEntry = getChildEntry(root, entry, null);


        if(newChildEntry == null) {
            return;
        } else {
            IndexNode<K,T> newRoot = new IndexNode<K,T>(newChildEntry.getKey(), root,
                    newChildEntry.getValue());
            root = newRoot;
            return;
        }
    }

    private Entry<K, Node<K,T>> getChildEntry(Node<K,T> node, Entry<K, Node<K,T>> entry,
                                              Entry<K, Node<K,T>> newChildEntry) {
        if(!node.isLeafNode) {
            // Choose subtree, find i such that Ki <= entry's key value < J(i+1)
            IndexNode<K,T> index = (IndexNode<K,T>) node;
            int i = 0;
            while(i < index.keys.size()) {
                if(entry.getKey().compareTo(index.keys.get(i)) < 0) {
                    break;
                }
                i++;
            }
            // Recursively, insert entry
            newChildEntry = getChildEntry((Node<K,T>) index.children.get(i), entry, newChildEntry);

            // Usual case, didn't split child
            if(newChildEntry == null) {
                return null;
            }
            // Split child case, must insert newChildEntry in node
            else {
                int j = 0;
                while (j < index.keys.size()) {
                    if(newChildEntry.getKey().compareTo(index.keys.get(j)) < 0) {
                        break;
                    }
                    j++;
                }

                index.insert(newChildEntry, j);

                // Usual case, put newChildEntry on it, set newChildEntry to null, return
                if(!index.isOverflowed()) {
                    return null;
                }
                else{
                    newChildEntry = splitIndexNode(index);

                    // Root was just split
                    if(index == root) {
                        // Create new node and make tree's root-node pointer point to newRoot
                        IndexNode<K,T> newRoot = new IndexNode<K,T>(newChildEntry.getKey(), root,
                                newChildEntry.getValue());
                        root = newRoot;
                        return null;
                    }
                    return newChildEntry;
                }
            }
        }
        // Node pointer is a leaf node
        else {
            LeafNode<K,T> leaf = (LeafNode<K,T>)node;
            LeafNode<K,T> newLeaf = (LeafNode<K,T>)entry.getValue();

            leaf.insert(entry.getKey(), newLeaf.valueOffsets.get(0));
            root.liveNode = leaf;
            // Usual case: leaf has space, put entry and set newChildEntry to null and return
            if(!leaf.isOverflowed()) {
                return null;
            }
            // Once in a while, the leaf is full
            else {
                newChildEntry = splitLeafNode(leaf);
                if(leaf == root) {
                    IndexNode<K,T> newRoot = new IndexNode<K,T>(newChildEntry.getKey(), leaf,
                            newChildEntry.getValue());
                    root = newRoot;
                    return null;
                }
                return newChildEntry;
            }
        }
    }

    /**
     * TODO Split a leaf node and return the new right node and the splitting
     * key as an Entry<slitingKey, RightNode>
     *
     * @param leaf, any other relevant data
     * @return the key/node pair as an Entry
     */
    public Entry<K, Node<K,T>> splitLeafNode(LeafNode<K,T> leaf) {
        ArrayList<K> newKeys = new ArrayList<K>();
        ArrayList<T> newValues = new ArrayList<T>();

        // The rest D entries move to brand new node
        for(int i=D; i<=2*D; i++) {
            newKeys.add(leaf.keys.get(i));
            newValues.add(leaf.valueOffsets.get(i));
        }

        // First D entries stay
        for(int i=D; i<=2*D; i++) {
            leaf.keys.remove(leaf.keys.size()-1);
            leaf.valueOffsets.remove(leaf.valueOffsets.size()-1);
        }

        K splitKey = newKeys.get(0);
        LeafNode<K,T> rightNode = new LeafNode<K,T>(newKeys, newValues);

        // Set sibling pointers
        LeafNode<K,T> tmp = leaf.nextLeaf;
        leaf.nextLeaf = rightNode;
        leaf.nextLeaf.previousLeaf = rightNode;
        rightNode.previousLeaf = leaf;
        rightNode.nextLeaf = tmp;

        Entry<K, Node<K,T>> newChildEntry = new AbstractMap.SimpleEntry<K, Node<K,T>>(splitKey, rightNode);

        return newChildEntry;
    }

    /**
     * TODO split an indexNode and return the new right node and the splitting
     * key as an Entry<slitingKey, RightNode>
     *
     * @param index, any other relevant data
     * @return new key/node pair as an Entry
     */
    public Entry<K, Node<K,T>> splitIndexNode(IndexNode<K,T> index) {
        ArrayList<K> newKeys = new ArrayList<K>();
        ArrayList<Node<K,T>> newChildren = new ArrayList<Node<K,T>>();

        // Note difference with splitting leaf page, 2D+1 key valueOffsets and 2D+2 node pointers
        K splitKey = index.keys.get(D);
        index.keys.remove(D);

        // First D key valueOffsets and D+1 node pointers stay
        // Last D keys and D+1 pointers move to new node
        newChildren.add(index.children.get(D+1));
        index.children.remove(D+1);

        while(index.keys.size() > D) {
            newKeys.add(index.keys.get(D));
            index.keys.remove(D);
            newChildren.add(index.children.get(D+1));
            index.children.remove(D+1);
        }

        IndexNode<K,T> rightNode = new IndexNode<K,T>(newKeys, newChildren);
        Entry<K, Node<K,T>> newChildEntry = new AbstractMap.SimpleEntry<K, Node<K,T>>(splitKey, rightNode);

        return newChildEntry;
    }

    /**
     * Delete method would associate a null value with the timestamp at which
     * delete was called
     *
     * @param key
     */
    public void delete(K key) {
        insert(key, (T) null);
    }
}
