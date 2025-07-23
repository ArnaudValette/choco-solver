package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.types.PairBasedStrategy;
import org.chocosolver.solver.propagation.consistencyStrategy.types.PassConsumer;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;

public class SACNaiveDriverStrategy extends PairBasedStrategy implements PassConsumer {
    public boolean consumePasses = false;
    public SACNaiveDriverStrategy(){
        super();
    }

    public SACNaiveDriverStrategy consumePasses(){
        consumePasses=true;
        return this;
    }

    public SACNaiveDriverStrategy ignorePasses(){
        consumePasses=false;
        return this;
    }

    public SACNaiveDriverStrategy(ReinitialisableQueue<Pair<IntVar,Integer>> Q) {
        super(Q);
    }

    public void centralRoutine(SingletonConsistencyEngine E) throws ContradictionException {
        E.doPropagate();
    }

    @Override
    public boolean willConsumePasses() {
        return consumePasses;
    }

    private void initState(SingletonConsistencyEngine E){
        E.setDoConsumePasses(false);
        E.setCheckSingleton(false);
        E.setDoFilterScheduling(false);
        E.setBlockLateScheduling(true);
    }

    @Override
    public void propagate(SingletonConsistencyEngine E) throws ContradictionException {
        super.propagate(E);
        initState(E);

        Q.reinit();
        centralRoutine(E);

        while (!Q.isEmpty()) {


            Pair<IntVar, Integer> p = Q.pop();
            IntVar v = p.getA();
            Integer val = p.getB();

            E.worldPush();
            IntDecision d = E.decide(v, val);
            d.buildNext();

            try {
                d.apply();
                onBeforePasses(E);
                centralRoutine(E);
                doConsumePasses(E);

                E.worldPop();
            } catch (ContradictionException e) {
                E.worldPopNFlush();
                v.removeValue(val, E);

                onAfterPasses(E);

                centralRoutine(E);
            }
        }
    }
}
