package org.chocosolver.solver.propagation.consistencyStrategy;


import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;

public abstract class BaseStrategy<T>  implements ISingletonConsistencyStrategy{
    protected ReinitialisableQueue<T> Q;
    protected boolean changed=false;

    public abstract void setQ(ReinitialisableQueue<T> Q);
    public abstract void propagate(SingletonConsistencyEngine E) throws ContradictionException;

    public abstract void loop() throws ContradictionException;
}
