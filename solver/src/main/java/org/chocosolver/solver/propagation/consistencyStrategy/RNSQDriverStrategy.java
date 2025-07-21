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
        E.setCheckSingleton(false); /* Don't check for singletons */
        E.setBlockLateScheduling(true); /* Don't late schedule */
        E.doPropagate();
    }

    private void conditionFC(SingletonConsistencyEngine E) throws ContradictionException {
        E.setBlockLateScheduling(false); /* We need to store NSAC propagators in lateQ */
        E.setSingleton(false); /* Init */
        E.setCheckSingleton(true); /* Check for singleton */
        E.doPropagate();
    }

    private void neighborhoodAC(SingletonConsistencyEngine E) throws ContradictionException {
        E.setBlockLateScheduling(true); /* Don't late schedule */
        E.setCheckSingleton(false); /* Don't check for singletons */
        E.doPropagate();
    }

    public void propagate(SingletonConsistencyEngine E) throws ContradictionException {
        E.setDoFilterScheduling(true); /* Do filter propagators */
        Q.reinit();
        E.freeDirectPropsSchedulingBL(); /* Don't blacklist anything for AC */
        ACenforce(E);

        while(!Q.isEmpty()){

            boolean changed = false;
            IntVar v = Q.pop();

            BitSet direct_only = E.getDirectOnlyBlacklist(v); /* Blacklist of propagators not related directly to V*/
            BitSet nsac_props = E.getNsacBlacklist(v); /* Blacklist of propagators not in neighborhood of V */
            Set<IntVar> nx = E.getNeighborhood(v); /* Neighborhood of V */

            for(int val = v.getLB(); val<=v.getUB(); val=v.nextValue(val)) {
                try {
                    E.worldPush();
                    IntDecision d = E.decide(v, val);
                    d.buildNext();
                    d.apply();


                    /* apply condition FC */

                    E.initLatePropQ(); /* empty Q */

                    E.setDirectPropsScheduling(direct_only); /* First filter : propagators directly related to V */
                    E.setLatePropsScheduling(nsac_props); /* Second filter (late schedule) : propagators in neighborhood of V */

                    /* Propagate AC on the sub-problem constituted with the direct neighbors of V;
                     * Every time choco wants to schedule a propagator,
                     * (a) propagator is directly related to V : scheduled
                     * (b) propagator is in neighborhood of V : late scheduled [latePropQ.add(p)]
                     * (c) none of the above : ignored
                     * */
                    conditionFC(E);

                    if(E.foundSingletonDuringPropagation()){
                        E.setDirectPropsScheduling(nsac_props); /* First filter : propagators in neighborhood of V */

                        /* Now, do schedule all late scheduled propagators */
                        while(!E.lateQisEmpty()){
                            Triple<Propagator<?>, Integer, Integer> args = E.latePropsPop();
                            E.imperativeSchedule(args.getFirst(), args.getSecond(), args.getThird()); /* force scheduling */
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
