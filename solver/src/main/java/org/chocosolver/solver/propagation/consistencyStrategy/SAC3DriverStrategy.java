package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;

public class SAC3DriverStrategy implements ISingletonConsistencyStrategy{
    ReinitialisableQueue<Pair<IntVar,Integer>> Q;

    public SAC3DriverStrategy(ReinitialisableQueue<Pair<IntVar, Integer>> Q) {
        this.Q = Q;
    }

    private void acEnforce(SingletonConsistencyEngine E) throws ContradictionException {
        E.setCheckSingleton(false); /* Don't check for singletons */
        E.setDoFilterScheduling(false); /* We propagate on the full problem */
        E.setBlockLateScheduling(true); /* No late scheduling */
        E.doPropagate();
    }
    public void propagate(SingletonConsistencyEngine E) throws ContradictionException {
        acEnforce(E);
        boolean changed = false;
        /* TODO: track when no changes have been made to the problem since last propagate call
        *   track whether the problem is already AC, or already SAC */
        Q.reinit();
        while (!Q.isEmpty()) {
            Pair<IntVar, Integer> p = Q.pop();
            IntVar v = p.getA();
            Integer val = p.getB();
            if (E.inModel(v, val)) {
                if (!buildBranch(E.decide(v, val), E)) {
                    v.removeValue(val, Cause.Null); // Throw means fail
                    acEnforce(E);
                    changed = true;
                }
            }
            if (Q.isEmpty() && changed) {
                Q.reinit();
                changed = false;
            }
        }
    }

    private boolean buildBranch(IntDecision d, SingletonConsistencyEngine E) {
        int id = E.getWorldIndex();
        try {
            E.worldPush();
            d.buildNext();
            d.apply();
            acEnforce(E);
        } catch (ContradictionException i) {
            E.worldPopUntilNFlush(id);
            return false;
        }
        int t = Q.size();
        int i = 0;
        while(i<t) {
            i++;
            /* Select a value (Xj, b) from PendingList Inter D, remove it from PendingList */
            Pair<IntVar, Integer> p = Q.pop();
            if(!E.inModel(p.getA(),p.getB())) {
                /* i.e. if value not in D, don't remove it from PendingList */
                Q.add(p);
                continue;
            }
            IntVar v = p.getA();
            Integer value = p.getB();
            try {
                E.worldPush();
                IntDecision d2 = E.decide(v, value);
                d2.buildNext();
                d2.apply();
                acEnforce(E);
            } catch (ContradictionException ignore) {
                Q.add(p);
                break;
            }
        }
        E.worldPopUntilNFlush(id);
        return true;
    }

}
