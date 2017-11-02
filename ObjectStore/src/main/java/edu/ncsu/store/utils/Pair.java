package edu.ncsu.store.utils;

import java.io.Serializable;

public class Pair<T1, T2> implements Serializable {
    private T1 first;
    private T2 second;

    public Pair(T1 t1, T2 t2) {
        first = t1;
        second = t2;
    }

    public T1 getFirst() {
        return first;
    }

    public T2 getSecond() {
        return second;
    }

    public void setFirst(T1 first) {
        this.first = first;
    }

    public void setSecond(T2 second) {
        this.second = second;
    }

    @Override
    public int hashCode() {
        return first.hashCode() * 17 + second.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Pair))
            return false;
        return first.equals(((Pair)obj).getFirst())  &&
                second.equals(((Pair) obj).getSecond());
    }

    public String toString() {
        return "(" + first.toString() + ", " + second.toString() + ")";
    }

}



