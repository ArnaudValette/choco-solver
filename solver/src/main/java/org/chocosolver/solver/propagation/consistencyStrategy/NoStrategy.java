package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.types.VariableBasedStrategy;

public class NoStrategy extends VariableBasedStrategy {
    boolean doPropagate=true;
    @Override
    public void passConsumer() throws ContradictionException {
    }

    public NoStrategy setDoPropagate(boolean doPropagate) {
        this.doPropagate = doPropagate;
        return this;
    }

    @Override
    public void propagate(SingletonConsistencyEngine engine) throws ContradictionException {
        if(doPropagate) {
            engine.doPropagate();
        }
    }
}
