package org.chocosolver.util.objects.queues;

import java.util.ArrayDeque;

public class ReinitialisableQueue<T> {
    /* TODO: this is restricted to Qset, it would be better to allow for several elements in q*/

    ArrayDeque<T> queue = new ArrayDeque<>();
    ArrayDeque<T> defaultQ = new ArrayDeque<>();
    private boolean locked = false;

    public ReinitialisableQueue(){}

    public void initAdd(T t){
        if(!locked){
            defaultQ.add(t);
        }
    }

    public void commitAndLock(){
        locked = true;
        queue = defaultQ.clone();
    }

    public void reinit(){
        queue = defaultQ.clone();
    }

    public boolean isEmpty(){
        return queue.isEmpty();
    }

    public T pop(){
        return queue.pop();
    }

    public void add(T t){
        queue.add(t);
    }

    public boolean contains(T t){
        return queue.contains(t);
    }

    public int size(){
        return queue.size();
    }

}
