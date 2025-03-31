/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.util.objects.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Data structure for a complete binary tree containing integers, with useful ready-to-use methods.
 *
 * @author Arthur Godet <arth.godet@gmail.com>
 * @since 31/07/2023
 */
public abstract class CompleteBinaryIntTree {
    public static int getTreeSizeFromNumberOfLeaves(int nbLeaves) {
        int size = 1;
        int powTwo = 2;
        while (nbLeaves > powTwo) {
            size += powTwo;
            powTwo *= 2;
        }
        return size + nbLeaves;
    }

    /**
     * Computes the min value between several elements.
     *
     * @param elements integer array
     * @return the minimum between all elements
     */
    public static int min(int... elements) {
        int min = elements[0];
        for (int i = 1; i < elements.length; i++) {
            min = Math.min(min, elements[i]);
        }
        return min;
    }

    /**
     * Computes the max value between several elements.
     *
     * @param elements integer array
     * @return the maximum between all elements
     */
    public static int max(int... elements) {
        int max = elements[0];
        for (int i = 1; i < elements.length; i++) {
            max = Math.max(max, elements[i]);
        }
        return max;
    }

    /**
     * Used to store the sorted nodes' id for a left-oriented depth-first-search.
     */
    protected int[] indexes;
    /**
     * Used to store the integer elements, that will be sorted before being inserted in the tree.
     */
    protected List<Integer> ids;
    /**
     * Indicates the node i in which the integer element is stored.
     */
    protected int[] pos;
    /**
     * Indicates the integer elements represented at the node i.
     */
    protected int[] at;
    /**
     * Indicates the id of the root node (0 if the current size is greater than maxSize / 2).
     */
    protected int root;

    /**
     * Creates a complete binary tree with ready-to-use internal structures, of the given maximum size.
     *
     * @param maxSize the maximum size of the tree
     */
    public CompleteBinaryIntTree(int maxSize) {
        indexes = new int[maxSize];
        ids = new ArrayList<>(maxSize);
        fillIndices(0, 0);
        pos = new int[maxSize];
        at = new int[maxSize];
        root = 0;
    }

    /**
     * Returns the left child node of the node i.
     *
     * @param i the id of the node
     * @return the left child node of the node i
     */
    protected int left(int i) {
        return 2 * i + 1;
    }

    /**
     * Returns whether the node i has a left child node.
     *
     * @param i the id of the node
     * @return whether the node i has a left child node
     */
    protected boolean hasLeft(int i) {
        return left(i) < indexes.length;
    }

    /**
     * Returns the right child node of the node i.
     *
     * @param i the id of the node
     * @return the right child node of the node i
     */
    protected int right(int i) {
        return 2 * i + 2;
    }

    /**
     * Returns whether the node i has a right child node.
     *
     * @param i the id of the node
     * @return whether the node i has a right child node
     */
    protected boolean hasRight(int i) {
        return right(i) < indexes.length;
    }

    /**
     * Returns whether the node i is a leaf node.
     *
     * @param i the id of the node
     * @return whether the node i is a leaf node
     */
    protected boolean isLeaf(int i) {
        return !hasLeft(i) && !hasRight(i);
    }

    /**
     * Returns the parent node of the node i.
     *
     * @param i the id of the node
     * @return the parent node of the node i
     */
    protected int above(int i) {
        return (i - 1) / 2;
    }

    /**
     * Returns whether the node i has a parent node.
     *
     * @param i the id of the node
     * @return whether the node i has a parent node
     */
    protected boolean hasAbove(int i) {
        return i != 0;
    }

    protected boolean isLeftChild(int i) {
        return i == left(above(i));
    }

    /**
     * Fill the indexes array by node indexes with a left-oriented depth-first-search.
     *
     * @param i           the current node
     * @param currentSize the current size
     * @return the size after visiting the subtree whose root is the node i
     */
    private int fillIndices(int i, int currentSize) {
        int size = currentSize;
        if (hasLeft(i)) {
            size = fillIndices(left(i), size);
        }
        indexes[size++] = i;
        if (hasRight(i)) {
            size = fillIndices(right(i), size);
        }
        return size;
    }

    /**
     * Resets the node i.
     *
     * @param i the id of the node to reset
     */
    protected void reset(int i) {
        pos[i] = -1;
        at[i] = -1;
    }

    /**
     * Resets the tree structure.
     */
    public void reset() {
        for (int i = 0; i < at.length; i++) {
            reset(i);
        }
        ids.clear();
        root = 0;
    }

    /**
     * Returns true if b is in the right subtree from root a
     *
     * @param a node id
     * @param b node id
     * @return whether b is in the right subtree from root a
     */
    protected boolean isInRightSubtree(int a, int b) {
        if (!hasRight(a)) {
            return false;
        }
        int i = b;
        while (hasAbove(i)) {
            if (i == right(a)) {
                return true;
            }
            i = above(i);
        }
        return false;
    }

    /**
     * Update the node i.
     *
     * @param i the id of the node
     */
    protected abstract void updateAt(int i);

    /**
     * Update all nodes from a given node up to the root node.
     *
     * @param from the node from which the update process starts
     */
    protected void updateUpToRoot(int from) {
        int i = from;
        while (hasAbove(i) && i != root) {
            updateAt(i);
            i = above(i);
        }
        updateAt(i); // update at root node
    }
}
