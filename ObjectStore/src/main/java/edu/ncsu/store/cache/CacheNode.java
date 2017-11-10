package edu.ncsu.store.cache;

class CacheNode<T1, T2> {
    T1 t1;
    T2 t2;

    CacheNode<T1, T2> prev, next;

    CacheNode(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof LRUCache))
            return false;
        return ((CacheNode) o).t1 == t1 &&
                ((CacheNode) o).t2 == t2;
    }
}
