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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EfficientQueue<T> {
    Map<T, Integer> indexMap;
    T[] universe;
    int size;
    int[] mark;
    int[] ring;
    long head;
    long tail;
    int idx;
    int C;
    int mask;
    int epoch;

    public EfficientQueue(T[] u){
        /* universe: base queue */
        universe = u;
        size = u.length;
        idx=0;

        indexMap= new HashMap<>();

        /* mark: BitSet with generation handling */
        mark = new int[size];
        epoch = 1;
        for(int i = 0; i<size; i++){
            indexMap.put(universe[i], i);
            mark[i] = 0; /* every element is in queued state */
        }

        /* Circular queue ring */
        C = (size <= 1) ? 1 : 1 << - Integer.numberOfLeadingZeros(size-1);
        mask = C-1;
        ring = new int[C];
        head=0L;
        tail=0L;
    }

    public boolean isEmpty() {
        return head==tail && idx == size;
    }

    public T pop() {
        if(idx < size){
            mark[idx] = epoch;
            T elem = universe[idx];
            idx++;
            return elem;
        } else if(head < tail){
            int i = ring[(int) (head & mask)];
            mark[i] = epoch;
            T elem = universe[i];
            head++;
            return elem;
        }
        return null;
    }

    public T peek() {
        if(idx < size){
            return universe[idx];
        } else if(head < tail){
            int i = ring[(int) (head & mask)];
            return universe[i];
        }
        return null;
    }

    public boolean contains(T t) {
        int i = getIndexOf(t);
        return mark[i] != epoch;
    }

    public int size() {
        return (size - idx) + (int) (tail - head);
    }

    public void add(T t) {
        int i = getIndexOf(t);
        if(mark[i] == epoch){
            ring[(int) (tail & mask)] = i;
            tail++;
            mark[i] = epoch-1;
        }
    }

    public void reinit() {
        head=0L;
        tail=0L;
        idx=0;
        if(++epoch == Integer.MIN_VALUE){
            /* Because it may happen */
            Arrays.fill(mark, 0);
            epoch=1;
        }
    }

    private int getIndexOf(T t) {
        Integer i = indexMap.get(t);
        if(i == null) throw new IllegalArgumentException();
        return i;
    }
}
