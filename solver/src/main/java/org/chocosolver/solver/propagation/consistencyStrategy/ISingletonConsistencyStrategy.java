package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;

public interface ISingletonConsistencyStrategy {
    public void propagate(SingletonConsistencyEngine engine) throws ContradictionException;
}
