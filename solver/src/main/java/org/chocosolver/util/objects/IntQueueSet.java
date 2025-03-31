/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.util.objects;

import java.util.Arrays;

/**
 * Data structure of a queue containing at most one element of integers between 0 and size-1.
 *
 * @author Arthur Godet <arth.godet@gmail.com>
 * @since 31/07/2023
 */
public class IntQueueSet {
    /**
     * The queue structure.
     */
    protected final int[] queue;
    /**
     * Used for the set's behaviour.
     */
    protected final boolean[] in;
    /**
     * The head of the queue.
     */
    protected int currentIndex;
    /**
     * The tail of the queue.
     */
    protected int lastIndex;

    /**
     * Creates a IntQueueSet for integers between 0 and maxSize-1.
     *
     * @param maxSize the maxSize of the IntQueueSet
     */
    public IntQueueSet(int maxSize) {
        this.queue = new int[maxSize];
        this.in = new boolean[maxSize];
    }

    /**
     * Removes all elements from the IntQueueSet.
     */
    public void clear() {
        currentIndex = 0;
        lastIndex = 0;
        Arrays.fill(in, false);
    }

    /**
     * Returns whether the IntQueueSet is empty or not.
     *
     * @return whether the IntQueueSet is empty or not
     */
    public boolean isEmpty() {
        return currentIndex == lastIndex && !in[queue[currentIndex]];
    }

    /**
     * Returns the size of the IntQueueSet.
     *
     * @return the size of the IntQueueSet
     */
    public int getSize() {
        return (lastIndex - currentIndex + queue.length) % queue.length;
    }

    /**
     * Returns true if the IntQueueSet contains integer i.
     *
     * @param i an integer
     * @return true if the IntQueueSet contains the integer
     */
    public boolean contains(int i) {
        return 0 <= i && i < in.length && in[i];
    }

    /**
     * Adds integer i at the end of the IntQueueSet.
     *
     * @param i an integer
     */
    public void add(int i) {
        if (0 <= i && i < in.length && !in[i]) {
            in[i] = true;
            queue[lastIndex++] = i;
            if (lastIndex == queue.length) {
                lastIndex = 0;
            }
        }
    }

    /**
     * Removes and returns the integer at the head of the IntQueueSet.
     *
     * @return the integer at the head of the IntQueueSet
     */
    public int remove() {
        int val = -1;
        if (!isEmpty()) {
            val = queue[currentIndex];
            if (currentIndex != lastIndex || in[queue[(currentIndex + 1) % queue.length]]) {
                currentIndex++;
            }
            in[val] = false;
            if (currentIndex == queue.length) {
                currentIndex = 0;
            }
        }
        return val;
    }
}
