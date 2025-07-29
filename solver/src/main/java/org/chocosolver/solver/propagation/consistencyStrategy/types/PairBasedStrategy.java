package org.chocosolver.solver.propagation.consistencyStrategy.types;

import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.AbstractSingletonStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;

public abstract class PairBasedStrategy extends AbstractSingletonStrategy<Pair<IntVar,Integer>> {

    public void setQ(ReinitialisableQueue<Pair<IntVar,Integer>> q) {
        Q=q;
    }

    @Override
    public void propagate(SingletonConsistencyEngine E) throws ContradictionException {
        if(Q == null){
            E.provideQ(this);
        }
        super.propagate(E);
    }

    public void loop() throws ContradictionException {
        while(!Q.isEmpty()){
            Pair<IntVar, Integer> p = Q.pop();
            Xi = p.getA(); Aj = p.getB();
            task();
        }
    }
}
