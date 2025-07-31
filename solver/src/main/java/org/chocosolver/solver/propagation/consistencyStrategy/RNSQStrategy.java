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
    ArrayList<Long> runsData = new ArrayList<>();

    @Override
    public void propagate(SingletonConsistencyEngine engine) throws ContradictionException {
        if(Q ==null){
            engine.provideQ(this);
        }
        changed=false;
        E=engine;
        onBeforeAnything();
        E.doPropagate();
        if(!initialized){
            Q.reinit();
            initialized = true;
        }
        else{
            Q.optimizedRefresh((IntVar) E.getLastDecision());
        }
        loop();

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

    protected void onAfterSingletonFound(){
        E.setBlockLateScheduling(true);
        E.setCheckSingleton(false);
        E.setDirectPropsScheduling(nsac);
        while (!E.lateQisEmpty()) {
            Triple<Propagator<?>, Integer, Integer> args = E.latePropsPop();
            E.doSchedule(args.getFirst(), args.getSecond(), args.getThird());
        }
    }

    @Override
    protected void onAfterInstantiation() throws ContradictionException {
        E.doPropagate();
        if (E.foundSingletonDuringPropagation()) {
            onAfterSingletonFound();
            E.doPropagate();
        }
        E.worldPopUntilNFlush(lastId);
        baseState();
    }


    @Override
    protected void onAfterRemoval() throws ContradictionException {
        changed=true;
    }

    @Override
    protected boolean queueHandler(boolean changed){
        //long start = System.nanoTime();
        if(changed){
            for(IntVar u : nx){
                if(!Q.contains(u)) {
                    Q.add(u);
                }
            }
        }
        //long end = System.nanoTime();
        //runsData.add(end - start);
        return changed;
    }

    public Pair<ArrayList<Long>, Integer> profile(){
        return new Pair<>(runsData, runsData.size());
    }

    @Override
    protected void passConsumer() throws ContradictionException {
        /* TODO: passes*/
    }

}
