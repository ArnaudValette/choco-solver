package org.chocosolver.solver.propagation.consistencyStrategy.types;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.ISingletonConsistencyStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;

public abstract class VariableBasedStrategy implements ISingletonConsistencyStrategy {

    protected ReinitialisableQueue<IntVar> Q;

    public VariableBasedStrategy(ReinitialisableQueue<IntVar> Q){
        this.Q=Q;
    }

    public void setQ(ReinitialisableQueue<IntVar> q) {
        Q = q;
    }

    public VariableBasedStrategy(){}

    public void propagate(SingletonConsistencyEngine E) throws ContradictionException {
        if(Q == null){
            E.provideQ(this);
        }
    }
}
