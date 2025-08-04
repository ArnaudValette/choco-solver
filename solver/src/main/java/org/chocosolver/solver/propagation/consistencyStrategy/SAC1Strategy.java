package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.consistencyStrategy.types.AbstractSAC1;

public class SAC1Strategy extends AbstractSAC1 {
    @Override
    public void onAfterInstantiation() throws ContradictionException {
        E.doPropagate();
        E.worldPopUntilNFlush(lastId);
        ref().baseState();
    }

    @Override
    public void passConsumer() throws ContradictionException {
        E.doPropagate();
    }
}
