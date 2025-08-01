package org.chocosolver.solver.propagation.consistencyStrategy;

import java.util.BitSet;

public class NSAC1Strategy extends SAC1Strategy{

    @Override
    public void onBeforeInstantiation(){
        ref().baseState();
        E.setDirectPropsScheduling(nsac);
        E.setDoFilterScheduling(true);
    }
}
