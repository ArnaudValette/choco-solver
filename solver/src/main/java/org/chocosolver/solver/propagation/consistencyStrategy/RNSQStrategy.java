package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.types.VariableBasedStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.alg.util.Triple;

import java.util.ArrayList;

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
        E.doPropagate();
        if(!initialized){
            Q.reinit();
            initialized = true;
        }
        else{
            Q.optimizedRefresh((IntVar) E.getLastDecision());
        }
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
    public void onAfterInstantiationPropagation() throws ContradictionException {
        if (E.foundSingletonDuringPropagation()) {
            ref().onAfterSingletonFound();
            E.doPropagate();
        }
    }

    @Override
    public void onAfterInstantiation() throws ContradictionException {
        E.doPropagate();
        ref().onAfterInstantiationPropagation();
        E.worldPopUntilNFlush(lastId);
        ref().baseState();
    }


    @Override
    public void onAfterRemoval() throws ContradictionException {
        changed=true;
    }

    @Override
    public boolean queueHandler(boolean changed){
        if(changed){
            for(IntVar u : nx){
                if(!Q.contains(u)) {
                    Q.add(u);
                }
            }
        }
        return changed;
    }


    @Override
    public void passConsumer() throws ContradictionException {
        /* TODO: passes*/
    }

}
