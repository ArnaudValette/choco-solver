package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.types.PairBasedStrategy;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;

public class SACNaiveDriverStrategy extends PairBasedStrategy {

    public SACNaiveDriverStrategy(){
        super();
    }

    public SACNaiveDriverStrategy(ReinitialisableQueue<Pair<IntVar,Integer>> Q) {
        super(Q);
    }

    private void acEnforce(SingletonConsistencyEngine E) throws ContradictionException {
        E.doPropagate();
    }

    @Override
    public void propagate(SingletonConsistencyEngine E) throws ContradictionException {
        super.propagate(E);
        E.setDoConsumePasses(false);
        E.setCheckSingleton(false);
        E.setDoFilterScheduling(false);
        E.setBlockLateScheduling(true);
        Q.reinit();
        acEnforce(E);
        while (!Q.isEmpty()) {
            Pair<IntVar, Integer> p = Q.pop();
            IntVar v = p.getA();
            Integer val = p.getB();
            E.worldPush();
            IntDecision d = E.decide(v, val);
            d.buildNext();
            try {
                d.apply();
                acEnforce(E);
                E.worldPopNFlush();
            } catch (ContradictionException e) {
                E.worldPopNFlush();
                v.removeValue(val, E);
                acEnforce(E);
            }
        }
    }
}
