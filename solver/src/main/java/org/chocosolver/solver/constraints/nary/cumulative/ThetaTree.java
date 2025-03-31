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

import org.chocosolver.solver.variables.Task;
import org.chocosolver.util.objects.tree.CompleteBinaryIntTree;

import java.util.Comparator;
import java.util.List;

public class ThetaTree extends CompleteBinaryIntTree {
    protected int INF = Integer.MAX_VALUE / 2;

    protected boolean[] present;
    protected int[] est;
    protected int[] proc;
    protected int[] sigmaP;
    protected int[] ect;

    public ThetaTree(int maxSize) {
        super(maxSize);
        present = new boolean[maxSize];
        est = new int[maxSize];
        proc = new int[maxSize];
        sigmaP = new int[maxSize];
        ect = new int[maxSize];
    }

    @Override
    public void reset() {
        for (int i = 0; i < at.length; i++) {
            reset(i);
            present[i] = false;
            est[i] = 0;
            proc[i] = 0;
            sigmaP[i] = 0;
            ect[i] = -INF;
        }
        ids.clear();
    }

    protected void initData(List<Task> tasks) {
        if (ids.size() != tasks.size()) {
            ids.clear();
            for (int k = 0; k < tasks.size(); k++) {
                ids.add(k);
            }
        }
        ids.sort(Comparator.comparingInt(i -> tasks.get(i).getEst()));
        for (int k = 0; k < tasks.size(); k++) {
            int id = ids.get(k);
            int i = indexes[k];
            pos[id] = i;
            at[i] = id;
            this.est[i] = tasks.get(id).getEst();
            proc[i] = tasks.get(id).getDuration().getLB();
            sigmaP[i] = 0;
            ect[i] = -INF;
            present[i] = false;
        }
    }

    public void init(List<Task> tasks) {
        reset();
        initData(tasks);
    }

    protected void updateAt(int i) {
        int p = present[i] ? proc[i] : 0;
        int e = present[i] ? est[i] : -INF;
        sigmaP[i] = p;
        int ectLeft = -INF;
        int ectRight = -INF;
        int sigmaPRight = 0;
        if (hasLeft(i)) {
            sigmaP[i] += sigmaP[left(i)];
            ectLeft = ect[left(i)];
        }
        if (hasRight(i)) {
            sigmaP[i] += sigmaP[right(i)];
            ectRight = ect[right(i)];
            sigmaPRight = sigmaP[right(i)];
        }
        ect[i] = max(ectLeft + p + sigmaPRight, e + p + sigmaPRight, ectRight);
    }

    public void add(int id) {
        int i = pos[id];
        present[i] = true;
        updateUpToRoot(i);
    }

    public void remove(int id) {
        int i = pos[id];
        present[i] = false;
        updateUpToRoot(i);
    }

    public boolean isPresent(int id) {
        return present[pos[id]];
    }

    public int getEctWithout(int id) {
        if (!isPresent(id)) {
            return getEct();
        }
        remove(id);
        int ect = getEct();
        add(id);
        return ect;
    }

    public int getEct() {
        return ect[root];
    }

}
