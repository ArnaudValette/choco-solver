/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.cumulative;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.util.objects.IntQueueSet;
import org.chocosolver.util.objects.tree.CompleteBinaryIntTree;

import java.util.*;

/**
 * Implementation of a tree used for searching ascendant sets of an element, as described in:
 * Carlier, J., Pinson, E.: Adjustment of heads and tails for the job-shop problem. In: European Journal of Operational Research, Volume 78, pp. 146-161 (1994). https://doi.org/10.1016/0377-2217(94)90379-4
 *
 * @author Arthur Godet <arth.godet@gmail.com>
 * @since 31/07/2023
 */
public class AscendantSetSearchTree extends CompleteBinaryIntTree {
    protected int[] est;
    protected int[] p;
    protected int[] lct;

    protected int[] pPlus;
    protected int[] sigma;
    protected int[] tau;
    protected int[] ksi;
    protected int[] sc;

    protected int INF = Integer.MAX_VALUE / 2;
    protected IntQueueSet queue;
    protected int epsilon;
    protected int current;
    protected int ub;
    protected List<Integer> U;
    protected PriorityQueue<Integer> A;
    protected List<Integer> S;
    protected Set<Integer> D;
    protected Set<Integer> toRemove;

    public AscendantSetSearchTree(int maxSize) {
        super(maxSize);

        est = new int[maxSize];
        p = new int[maxSize];
        lct = new int[maxSize];
        pPlus = new int[maxSize];
        sigma = new int[maxSize];
        tau = new int[maxSize];
        ksi = new int[maxSize];
        sc = new int[maxSize];
        queue = new IntQueueSet(maxSize);
        U = new ArrayList<>(maxSize);
        A = new PriorityQueue<>(maxSize, Comparator.comparingInt(i -> lct[pos[i]]));
        S = new ArrayList<>(maxSize);
        D = new HashSet<>(maxSize);
        toRemove = new HashSet<>(maxSize);
        reset();
    }

    @Override
    public void reset() {
        for (int i = 0; i < at.length; i++) {
            reset(i);
            est[i] = 0;
            p[i] = 0;
            lct[i] = 0;
            pPlus[i] = 0;
            sigma[i] = 0;
            tau[i] = 0;
            ksi[i] = -INF;
            sc[i] = -INF;
        }
        ids.clear();
        queue.clear();
    }

    protected void initialize(List<Task> tasks) {
        queue.clear();
        if (ids.size() != tasks.size()) {
            ids.clear();
            for (int i = 0; i < tasks.size(); i++) {
                ids.add(i);
            }
        }
        ids.sort(Comparator.comparingInt(i -> -tasks.get(i).getLct())); // sort by decreasing lct
        root = at.length;
        ub = -INF;
        for (int k = 0; k < tasks.size(); k++) {
            int id = ids.get(k);
            int i = indexes[k];
            at[i] = id;
            pos[id] = i;
            if (root > i) {
                root = i; // root of the tree is the smallest index used to fill the tree
            }
            this.est[i] = tasks.get(id).getEst();
            this.p[i] = tasks.get(id).getDuration().getLB();
            this.lct[i] = tasks.get(id).getLct();
            if (ub < this.lct[i]) {
                ub = this.lct[i];
            }
            pPlus[i] = this.p[i];
            sigma[i] = 0;
            tau[i] = 0;
            ksi[i] = -INF;
            sc[i] = -INF;
            if (!hasLeft(i) && !hasRight(i)) {
                queue.add(i);
            }
        }
        ub++;
        // reset the rest of the tree
        for (int k = tasks.size(); k < indexes.length; k++) {
            int i = indexes[k];
            at[i] = -1;
            this.est[i] = -INF;
            this.p[i] = -1;
            this.lct[i] = INF;
            pPlus[i] = this.p[i];
            sigma[i] = 0;
            tau[i] = 0;
            ksi[i] = -INF;
            sc[i] = -INF;
        }
        epsilon = 0;
        current = -1;
        while (!queue.isEmpty()) {
            int i = queue.remove();
            sigma[i] = this.p[i] + (hasRight(i) ? tau[right(i)] : 0); // different value when initiating the tree
            tau[i] = this.p[i] + (hasLeft(i) ? tau[left(i)] : 0) + (hasRight(i) ? tau[right(i)] : 0);
            updateAt(i);
            if (i != root && hasAbove(i)) {
                queue.add(above(i));
            }
        }
    }

    @Override
    protected void updateAt(int i) {
        int ksiLeft = hasLeft(i) ? ksi[left(i)] : -INF;
        int ksiRight = hasRight(i) ? ksi[right(i)] : -INF;
        if (current == i || current >= 0 && (lct[i] > lct[current] || lct[i] == lct[current] && isInRightSubtree(i, current))) {
            sigma[i] += epsilon;
        }
        if (pPlus[i] == 0) {
            ksi[i] = Math.max(ksiLeft + sigma[i], ksiRight);
        } else {
            ksi[i] = max(ksiLeft + sigma[i], ub - lct[i] + sigma[i], ksiRight);
        }
    }

    protected void update(int k, int epsilon) {
        current = k;
        this.epsilon = epsilon;
        updateUpToRoot(k);
        current = -1;
        this.epsilon = 0;
    }

    protected int findSc(int c) {
        if (sc[c] != -INF) {
            return sc[c];
        }
        int i = pos[c]; // get the node containing data on c
        update(i, -pPlus[i]);
        int sc = INF;
        int delta = ub - (est[i] + p[i]);
        int k = root;
        while (sc == INF && ksi[k] > delta) {
            if ((hasLeft(k) ? ksi[left(k)] : -INF) + sigma[k] > delta) {
                delta -= sigma[k];
                k = left(k);
            } else {
                if ((ub - lct[k] + sigma[k] > delta) && pPlus[k] != 0) {
                    sc = k;
                } else if (hasRight(k)) {
                    k = right(k);
                } else {
                    break;
                }
            }
        }
        update(i, pPlus[i]);
        if (sc != INF) {
            return at[sc];
        } else {
            return sc;
        }
    }

    private int adjustInit(List<Task> tasks) {
        U.clear();
        A.clear();
        S.clear();
        D.clear();
        int t = INF;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getEst() < t) {
                t = tasks.get(i).getEst();
                U.addAll(A);
                A.clear();
                A.add(i);
            } else if (tasks.get(i).getEst() == t) {
                A.add(i);
            } else {
                U.add(i);
            }
            S.add(i);
        }
        return t;
    }

    /**
     * Adjusts the earliest start times of tasks in the list.
     *
     * @param tasks the list of tasks
     * @param prop  the propagator doing the filtering
     * @return whether a filtering has occurred
     * @throws ContradictionException
     */
    public boolean adjust(List<Task> tasks, PropagatorResource prop) throws ContradictionException {
        boolean hasFiltered = false;
        initialize(tasks);
        int t = adjustInit(tasks);
        // In our implementation, U is the set of elements j such that est[j] < t (otherwise elements are in A or D)
        U.sort(Comparator.comparingInt(i -> -tasks.get(i).getEst())); // more efficient to remove last elements of ArrayList than first ones
        S.sort(Comparator.comparingInt(i -> tasks.get(i).getLct()));
        while (!S.isEmpty()) {
            toRemove.clear();
            for (Integer c : A) {
                if (est[pos[c]] == t) {
                    int sc = findSc(c);
                    if (sc != INF) {
                        this.sc[c] = sc;
                        toRemove.add(c);
                        D.add(c);
                    }
                }
            }
            A.removeAll(toRemove);
            if (t >= ub) { // Might happen for infeasible schedules
                prop.fails();
            }
            // i is the element in A with minimum lct
            Integer i = A.peek();
            // tPrime is the smallest est[j] for j in U
            int tPrime = INF;
            if (!U.isEmpty()) {
                tPrime = tasks.get(U.get(U.size() - 1)).getEst();
            }
            int epsilon = i != null && i >= 0 ? Math.min(pPlus[pos[i]], tPrime - t) : tPrime - t;
            t += epsilon;
            if (i != null && i >= 0) {
                pPlus[pos[i]] -= epsilon;
                update(pos[i], -epsilon);
                if (pPlus[pos[i]] == 0) {
                    S.remove(i);
                    A.remove(i);
                }
            }
            // Update nu as the task with smallest lct such that pPlus > 0
            if (S.isEmpty()) {
                return hasFiltered;
            }
            int nu = S.get(0);
            // Update D
            toRemove.clear();
            for (Integer j : D) {
                int sj = findSc(j);
                if (sj >= 0 && pPlus[pos[sj]] == 0 || tasks.get(nu).getLct() > tasks.get(sj).getLct()) {
                    toRemove.add(j);
                    A.add(j);
                    sc[j] = -INF;
                    hasFiltered |= tasks.get(j).updateEst(t, prop);
                }
            }
            D.removeAll(toRemove);
            if (A.isEmpty() && !U.isEmpty()) {
                t = tasks.get(U.get(U.size() - 1)).getEst();
            }
            // Add to A all j in U such that est[j] == t
            while (!U.isEmpty() && tasks.get(U.get(U.size() - 1)).getEst() == t) {
                A.add(U.remove(U.size() - 1));
            }
        }
        return hasFiltered;
    }
}
