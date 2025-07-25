package org.chocosolver.util.objects.queues;

import java.util.ArrayDeque;
import java.util.function.Function;
import java.util.stream.Stream;

public class ReinitialisableQueue<T> {
    /* TODO: this is restricted to Qset, it would be better to allow for several elements in q*/

    ArrayDeque<T> queue = new ArrayDeque<>();
    ArrayDeque<T> defaultQ = new ArrayDeque<>();
    private boolean locked = false;
    Function<Void, ArrayDeque<T>> supplier;
    boolean hasSupplier= false;

    public ReinitialisableQueue(){}

    public void initAdd(T t){
        if(!locked){
            defaultQ.add(t);
        }
    }

    public void setSupplier(Function<Void, ArrayDeque<T>> s){
        if(!locked) {
            supplier = s;
            hasSupplier = true;
        }
    }

    public void commitAndLock(){
        locked = true;
        if(hasSupplier){
            queue = supplier.apply(null);
        }
        else {
            queue = defaultQ.clone();
        }
    }

    public void reinit(){
        if(hasSupplier){
            queue = supplier.apply(null);
        }
        else {
            queue = defaultQ.clone();
        }
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
