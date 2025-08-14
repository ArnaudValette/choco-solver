/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.propagation.consistencyStrategy.types;

import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.consistencyStrategy.AbstractSingletonStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.queues.EfficientQueue;

public abstract class PairBasedStrategy extends AbstractSingletonStrategy<Pair<IntVar,Integer>> {

    public void setQ(EfficientQueue<Pair<IntVar,Integer>> q) {
        Q=q;
    }

    public void loop() throws ContradictionException {
        while(!Q.isEmpty() && !E.shouldStop()){
            Pair<IntVar, Integer> p = Q.pop();
            Xi = p.getA(); Aj = p.getB();
            ref().task();
        }
    }
}
