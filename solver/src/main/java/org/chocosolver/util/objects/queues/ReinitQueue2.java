/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.util.objects.queues;


import java.util.ArrayDeque;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReinitQueue2<T> implements RQ<T> {
    ArrayDeque<T> queue;
    private boolean locked = false;
    Function<Void, Stream<T>> supplier;
    boolean hasSupplier= false;

    public ReinitQueue2(){}

    public void setSupplier(Function<Void, Stream<T>> s){
        if(!locked) {
            supplier = s;
            hasSupplier = true;
        }
    }

    public void lock(){
        locked = true;
    }

    public void reinit(){
        if(hasSupplier){
            queue = supplier.apply(null).collect(Collectors.toCollection(ArrayDeque::new));
        }
        else{
            throw new RuntimeException("Trying to reinitialise a ReinitialisableQueue without a supplier");
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
