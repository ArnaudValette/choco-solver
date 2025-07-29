package org.chocosolver.solver.propagation.consistencyStrategy;

import java.util.BitSet;

public class NSAC1Strategy extends SAC1Strategy{

    @Override
    protected void onBeforeInstantiation(){
        BitSet nsac = E.getNsacBlacklist(Xi);
        E.setDirectPropsScheduling(nsac);
        E.setDoFilterScheduling(true);
    }

}
