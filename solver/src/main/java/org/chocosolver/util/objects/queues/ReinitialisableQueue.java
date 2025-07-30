package org.chocosolver.util.objects.queues;

import java.util.LinkedList;
import java.util.function.Function;

public class ReinitialisableQueue<T> {
    /* TODO: this is restricted to Qset, it would be better to allow for several elements in q*/

    LinkedList<T> queue = new LinkedList<>();
    LinkedList<T> defaultQ = new LinkedList<>();
    private boolean locked = false;
    Function<Void, LinkedList<T>> supplier;
    boolean hasSupplier= false;

    public ReinitialisableQueue(){}

    public void initAdd(T t){
        if(!locked){
            defaultQ.add(t);
        }
    }

    public void setSupplier(Function<Void, LinkedList<T>> s){
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
            queue = (LinkedList<T>) defaultQ.clone();
        }
    }

    public void reinit(){
        if(hasSupplier){
            queue = supplier.apply(null);
        }
        else {
            queue = (LinkedList<T>) defaultQ.clone();
        }
    }

    public boolean isEmpty(){
        return queue.isEmpty();
    }

    public T pop(){
        return queue.pop();
    }

    public void add(T t){
        queue.addLast(t);
    }

    public boolean contains(T t){
        return queue.contains(t);
    }

    public int size(){
        return queue.size();
    }

}
