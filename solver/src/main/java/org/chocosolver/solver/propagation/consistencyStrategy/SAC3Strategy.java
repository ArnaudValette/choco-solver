/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.consistencyStrategy.types.PairBasedStrategy;
import org.chocosolver.solver.variables.IntVar;

public class SAC3Strategy extends PairBasedStrategy {

    @Override
    public void onAfterInstantiation() throws ContradictionException {
            E.doPropagate();
            ref().buildBranch();
            ref().baseState();
    }

    @Override
    public void buildBranch(){
        int i = 0;
        int t = Q.size();
        while(i<t) {
            Pair<IntVar, Integer> p = Q.pop();
            IntVar Xn = p.getA();
            Integer Am = p.getB();
            i++;
            if(!Xn.contains(Am)){
                Q.add(p);
                continue;
            }
            try {
                instantiate(Xn, Am);
                E.doPropagate();
            } catch (ContradictionException ignore) {
                Q.add(p);
                break;
            }
        }
        if(i==t){
            boolean isSolved = true;
            for(IntVar v : E.getModel().retrieveIntVars(true)){
                if(!v.isInstantiated()){
                    isSolved = false;
                }
            }
            if(isSolved){
                solved=true;
                return;
            }
        }
        E.worldPopUntilNFlush(lastId);
    }

    @Override
    public void passConsumer() throws ContradictionException {
        /* TODO k-pSAC3 */
    }
}
