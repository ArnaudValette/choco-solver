package org.chocosolver.solver.propagation.consistencyStrategy.types;

import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.ISingletonConsistencyStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;

public abstract class PairBasedStrategy implements ISingletonConsistencyStrategy {
    protected ReinitialisableQueue<Pair<IntVar,Integer>> Q;
    public PairBasedStrategy(ReinitialisableQueue<Pair<IntVar, Integer>> Q) {
        this.Q = Q;
    }

    public void setQ(ReinitialisableQueue<Pair<IntVar, Integer>> q) {
        Q = q;
    }

    public PairBasedStrategy(){}

    public void propagate(SingletonConsistencyEngine E) throws ContradictionException {
        if(Q == null){
            E.provideQ(this);
        }
    }
}
