package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;
import org.jgrapht.alg.util.Triple;

import java.util.BitSet;
import java.util.Set;

public class RNSQDriverStrategy implements ISingletonConsistencyStrategy {
    ReinitialisableQueue<IntVar> Q;

    public RNSQDriverStrategy(ReinitialisableQueue<IntVar> Q){
        this.Q= Q;
    }
    private void ACenforce(SingletonConsistencyEngine E) throws ContradictionException {
        E.setCheckSingleton(false);
        E.setBlockLateScheduling(true);
        E.doPropagate();
    }

    private void conditionFC(SingletonConsistencyEngine E) throws ContradictionException {
        E.setBlockLateScheduling(false);
        E.setSingleton(false);
        E.setCheckSingleton(true);
        E.doPropagate();
    }

    private void neighborhoodAC(SingletonConsistencyEngine E) throws ContradictionException {
        E.setBlockLateScheduling(true);
        E.setCheckSingleton(false);
        E.doPropagate();
    }

    public void propagate(SingletonConsistencyEngine E) throws ContradictionException {
        E.setDoFilterScheduling(true);
        Q.reinit();
        E.freeDirectPropsSchedulingBL();
        ACenforce(E);

        while(!Q.isEmpty()){

            boolean changed = false;
            IntVar v = Q.pop();

            BitSet direct_only = E.getDirectOnlyBlacklist(v);
            BitSet nsac_props = E.getNsacBlacklist(v);
            Set<IntVar> nx = E.getNeighborhood(v);

            for(int val = v.getLB(); val<=v.getUB(); val=v.nextValue(val)) {
                try {
                    E.worldPush();
                    IntDecision d = E.decide(v, val);
                    d.buildNext();
                    d.apply();


                    /* apply condition FC */

                    E.initLatePropQ();

                    E.setDirectPropsScheduling(direct_only);
                    E.setLatePropsScheduling(nsac_props);

                    conditionFC(E);

                    if(E.foundSingletonDuringPropagation()){
                        E.setDirectPropsScheduling(nsac_props);

                        while(!E.lateQisEmpty()){
                            Triple<Propagator<?>, Integer, Integer> args = E.latePropsPop();
                            E.imperativeSchedule(args.getFirst(), args.getSecond(), args.getThird());
                        }
                        /* apply AC to N(v) */
                        neighborhoodAC(E);
                    }

                    E.worldPopNFlush();
                } catch (ContradictionException e) {
                    /* DOM-WIPEOUT */
                    E.worldPopNFlush();
                    v.removeValue(val, Cause.Null);
                    changed=true;
                }
            }
            if(changed){
                /* add neighbors to the queue */
                for(IntVar u : nx){
                    if(!Q.contains(u)){
                        Q.add(u);
                    }
                }
            }
        }
    }
}
