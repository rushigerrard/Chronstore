package edu.ncsu.store.cache;

public class CacheNodeList<T1, T2> {

    CacheNode<T1, T2> head, tail;

    CacheNodeList() {
        head = null;
        tail = null;
    }

    void insert(CacheNode<T1, T2> node) {
        if (head == null) {
            head = node;
            node.next = null;
            node.prev = null;
            tail = head;
        } else {
            tail.next = node;
            node.next = null;
            node.prev = tail;
            tail = node;
        }
    }

    void remove(CacheNode<T1, T2> node) {
        if (node == head)
            head = head.next;
        if (node == tail)
            tail = tail.prev;
        if (node.next != null)
            node.next.prev = node.prev;
        if (node.prev != null)
            node.prev.next = node.next;
    }
}
