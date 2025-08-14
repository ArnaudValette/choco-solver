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

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.types.VariableBasedStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.jgrapht.alg.util.Triple;


public class RNSQStrategy extends VariableBasedStrategy {
    boolean initialized = false;

    @Override
    public void propagate(SingletonConsistencyEngine engine) throws ContradictionException {
        if(Q ==null){
            engine.provideQ(this);
        }
        changed=false;
        E=engine;
        ref().onBeforeAnything();
        ref().basePropagation();
        initialized = true;
        Q.reinit();
        ref().loop();

    }

    @Override
    public void onBeforeInstantiation(){
        E.initLatePropQ();
        E.setDirectPropsScheduling(rnsac);
        E.setLatePropsScheduling(nsac);
        E.setDoFilterScheduling(true);
        E.setBlockLateScheduling(false);
        E.setSingleton(false);
        E.setCheckSingleton(true);
    }

    @Override
    public void onAfterInstantiation() throws ContradictionException {
        ref().onAfterInstantiationPropagation();
        E.worldPopUntilNFlush(lastId);
        ref().baseState();
    }

    @Override
    public void onAfterInstantiationPropagation() throws ContradictionException {
        /* if a neighbor of xi has singleton domain then apply AC to N(xi) */
        if (E.foundSingletonDuringPropagation()) {
            ref().onAfterSingletonFound();
            ref().onBeforePasses();
            ref().basePropagation();
            ref().doConsumePasses();
        }
    }

    @Override
    public void onAfterSingletonFound(){
        E.setBlockLateScheduling(true);
        E.setCheckSingleton(false);
        E.setDirectPropsScheduling(nsac);
        while (!E.lateQisEmpty()) {
            Triple<Propagator<?>, Integer, Integer> args = E.latePropsPop();
            E.doSchedule(args.getFirst(), args.getSecond(), args.getThird());
        }
    }



    @Override
    public void onAfterRemoval() throws ContradictionException {
        changed=true;
    }

    @Override
    public boolean queueHandler(boolean changed){
        if(changed){
            for(IntVar u : nx){
                Q.add(u);
            }
        }
        return changed;
    }


    @Override
    public void passConsumer() throws ContradictionException {
        ref().basePropagation();
    }

    @Override
    public boolean _isNeighborhoodAlgo() {
        return true;
    }
}
