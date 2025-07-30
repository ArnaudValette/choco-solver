package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.consistencyStrategy.types.VariableBasedStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.jgrapht.alg.util.Triple;

public class RNSQStrategy extends VariableBasedStrategy {
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

    private void prepareState(){
        E.setBlockLateScheduling(true);
        E.setCheckSingleton(false);
        E.setDirectPropsScheduling(nsac);
    }

    @Override
    protected void onAfterInstantiation() throws ContradictionException {
        E.doPropagate();
        if (E.foundSingletonDuringPropagation()) {
            prepareState();
            while (!E.lateQisEmpty()) {
                Triple<Propagator<?>, Integer, Integer> args = E.latePropsPop();
                E.doSchedule(args.getFirst(), args.getSecond(), args.getThird());
            }
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
    protected void passConsumer() throws ContradictionException {
        /* TODO: passes*/
    }

}
