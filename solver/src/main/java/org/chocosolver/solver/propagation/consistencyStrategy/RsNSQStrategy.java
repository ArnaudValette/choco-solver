package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.constraints.Propagator;
import org.jgrapht.alg.util.Triple;

import java.util.BitSet;

public class RsNSQStrategy extends RNSQStrategy{
    BitSet subGraph;
    @Override
    public void onBeforeInstantiation() {
        super.onBeforeInstantiation();
        E.freeLatePropsSchedulingBL();
        E.reinitSubNeighborhoodBlacklist();
    }

    @Override
    public void onAfterSingletonFound() {
        subGraph = E.getSubNeighborhoodBlacklist();
        E.setBlockLateScheduling(true);
        E.setCheckSingleton(false);
        E.setDirectPropsScheduling(subGraph);
        while (!E.lateQisEmpty()) {
            Triple<Propagator<?>, Integer, Integer> args = E.latePropsPop();
            if(!subGraph.get(args.getFirst().hashCode()))
                E.doSchedule(args.getFirst(), args.getSecond(), args.getThird());
        }
    }
}
