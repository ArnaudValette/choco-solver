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
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.variables.IntVar;

public abstract class AbstractSAC1 extends VariableBasedStrategy{
    @Override
    public void loop() throws ContradictionException {
        /* This is the reason this class exist,
         * Using SAC1 with the VariableBasedStrategy.loop() logic
         * is not a valid SAC enforcing algorithm.
         * */
        boolean shouldStop = false;
        do{
            Q.reinit();
            changed = false;
            while (!Q.isEmpty()) {
                IntVar v = Q.pop();
                nsac = E.getNsacBlacklist(v);
                nx = E.getNeighborhood(v);
                for (int value = v.getLB(); value <= v.getUB(); value = v.nextValue(value)) {
                    Xi = v;
                    Aj = value;
                    ref().task();
                }
            }
            if(willConsumePasses) {
                E.passesIncrement();
                boolean reachedPassesEnd = !E.hasPasses();
                shouldStop = willConsumePasses && reachedPassesEnd;
            }
        } while (changed && !shouldStop);
    }

    @Override
    public boolean queueHandler(boolean changed){
        /* Don't do anything fancy */
        return changed;
    }
}
