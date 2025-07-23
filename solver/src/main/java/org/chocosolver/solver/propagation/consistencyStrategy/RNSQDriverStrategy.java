package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.types.VariableBasedStrategy;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;
import org.jgrapht.alg.util.Triple;

import java.util.BitSet;
import java.util.Set;

public class RNSQDriverStrategy extends VariableBasedStrategy {
    boolean subNeighborhood=false;
    boolean consumePasses=false;

    public RNSQDriverStrategy restrictToSubNeighborhood(SingletonConsistencyEngine E){
        subNeighborhood = true;
        E.setCollectSingleton(true);
        return this;
    }

    public RNSQDriverStrategy consumePasses(){
        consumePasses = true;
        return this;
    }

    public RNSQDriverStrategy unrestrictToSubNeighborhood(SingletonConsistencyEngine E){
        subNeighborhood = false;
        E.setCollectSingleton(false);
        return this;
    }

    public RNSQDriverStrategy ignorePasses(){
        consumePasses=false;
        return this;
    }

    public RNSQDriverStrategy(){

    }

    public RNSQDriverStrategy(ReinitialisableQueue<IntVar> Q){
        super(Q);
    }

    private void ACenforce(SingletonConsistencyEngine E) throws ContradictionException {
        E.setCheckSingleton(false); /* Don't check for singletons */
        E.setBlockLateScheduling(true); /* Don't late schedule */
        E.doPropagate();
    }

    private void conditionFC(SingletonConsistencyEngine E) throws ContradictionException {
        E.setBlockLateScheduling(false); /* We need to store late propagators in lateQ */
        E.setSingleton(false); /* Init */
        E.setCheckSingleton(true); /* Check for singleton */
        E.doPropagate();
    }

    private void neighborhoodAC(SingletonConsistencyEngine E) throws ContradictionException {
        E.setBlockLateScheduling(true); /* Don't late schedule */
        E.setCheckSingleton(false); /* Don't check for singletons */
        E.doPropagate();
    }


    @Override
    public void propagate(SingletonConsistencyEngine E) throws ContradictionException {
        super.propagate(E);
        E.setDoConsumePasses(false);
        E.setDoFilterScheduling(true); /* Do filter propagators */
        E.freeDirectPropsSchedulingBL(); /* Don't blacklist anything for AC */
        /* i.e. no filtering (cleared blacklist) */

        Q.reinit();
        ACenforce(E);

        while(!Q.isEmpty()){

            boolean changed = false;
            IntVar v = Q.pop();

            BitSet direct_only = E.getDirectOnlyBlacklist(v); /* Blacklist of propagators not related directly to V*/
            BitSet nsac_props = E.getNsacBlacklist(v); /* Blacklist of propagators not in neighborhood of V */
            Set<IntVar> nx = E.getNeighborhood(v); /* Neighborhood of V */

            for(int val = v.getLB(); val<=v.getUB(); val=v.nextValue(val)) {
                try {
                    E.setDoConsumePasses(false);
                    E.worldPush();
                    IntDecision d = E.decide(v, val);
                    d.buildNext();
                    d.apply();


                    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * **/
                    /* apply condition FC */

                    E.initLatePropQ(); /* empty latePropQ */

                    E.setDirectPropsScheduling(direct_only); /* First filter : propagators directly related to V */

                    if(subNeighborhood){
                        /* In RsNSAC, we don't know -a priori- which propagators we want to keep,
                         * while we don't know which D(Xi) are singleton, we may want to run ANY propagator
                         * */
                        E.freeLatePropsSchedulingBL(); /* new BitSet (clear) */
                        E.reinitSubNeighborhoodBlacklist(); /* initialize */
                    }
                    else {
                        /* RNSAC */
                        E.setLatePropsScheduling(nsac_props); /* Second filter (late schedule) : propagators in neighborhood of V */
                    }

                    /* Propagate AC on the sub-problem constituted with the direct neighbors of V;
                     * Every time choco wants to schedule a propagator,
                     * RNSAC:
                     * (a) propagator is directly related to V : scheduled
                     * (b) propagator is in neighborhood of V : late scheduled [latePropQ.add(p)]
                     * (c) none of the above : ignored
                     * RsNAC:
                     * (a) propagator is directly related to V : scheduled
                     * (b) propagator is none of the above : late -filtered- (more on that later)
                     * */
                    conditionFC(E);

                    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * **/
                    if(E.foundSingletonDuringPropagation()){
                        /* When the engine is in collectSingleton mode,
                        * this blacklist filters every propagator that doesn't concern
                        * variables that became singleton during the previous step */
                        BitSet subGraph = E.getSubNeighborhoodBlacklist();

                        if(subNeighborhood){
                            /* RsNSAC : restrict our problem to this sub-graph */
                            E.setDirectPropsScheduling(subGraph);
                        }
                        else {
                            /* RNSAC */
                            E.setDirectPropsScheduling(nsac_props); /* First filter : propagators in neighborhood of V */
                        }

                        /** Pass based algorithms :
                         * if consumePasses is set to true,
                         * then the engine will fill the list of passPropagators
                         * scheduling exactly ONCE the propagators we want to schedule.
                         * This list is then imperatively scheduled as many time we need (pass consumption)
                         * after having called neighborhoodAC ONCE.
                         * The passBlackList avoid propagators to be scheduled twice, and since we
                         * are working with a restricted set of propagators (NSAC/sNSAC)
                         * the side effects are controlled ({@link SingletonConsistencyEngine#schedule(Propagator, int, int)}
                         * */
                        E.reinitPassBlacklist();
                        E.passesInit();
                        E.setDoConsumePasses(consumePasses);
                        E.initPassPropList();

                            /* Now, do schedule all late scheduled propagators (RNSAC)
                             * otherwise (RsNSAC) filter late scheduled propagators
                             * */
                            while (!E.lateQisEmpty()) {
                                Triple<Propagator<?>, Integer, Integer> args = E.latePropsPop(); /* Consume Q */
                                if (subNeighborhood) {
                                    if (!subGraph.get(args.getFirst().hashCode())) {
                                        E.doSchedule(args.getFirst(), args.getSecond(), args.getThird()); /* force scheduling */
                                    }
                                } else {
                                    E.doSchedule(args.getFirst(), args.getSecond(), args.getThird()); /* force scheduling */
                                }
                            }

                            /* apply AC to N(v) or SD(v),
                            * if consumePasses is true, this will perform a single pass AC */
                            neighborhoodAC(E);
                            if(consumePasses){
                                /* Repeat the previous operation:
                                * consumes all passes from the engine
                                * by rescheduling the previously executed propagators */
                                E.passesIncrement();
                                while(E.hasPasses()){
                                    for(int i =0; i<E.passPropagatorsSize(); i++){
                                        Triple<Propagator<?>, Integer, Integer> args = E.passQueueAt(i);
                                        E.imperativeSchedule(args.getFirst(), args.getSecond(), args.getThird());
                                    }
                                    neighborhoodAC(E);
                                    E.passesIncrement();
                                }
                                E.reinitPassBlacklist();
                                E.initPassPropList();
                                E.passesInit();
                                E.setDoConsumePasses(false);
                            }
                    }

                    E.worldPopNFlush();
                } catch (ContradictionException e) {
                    /* DOM-WIPEOUT */
                    E.worldPopNFlush();
                    v.removeValue(val, E);
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
