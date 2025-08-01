package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;

public interface ISingletonConsistencyStrategy {

   void propagate(SingletonConsistencyEngine engine) throws ContradictionException;

    void loop() throws ContradictionException;
    void task() throws ContradictionException;
    boolean queueHandler(boolean changed);

    ISingletonConsistencyStrategy ref();
    void setRef(ISingletonConsistencyStrategy ref);
    void onBeforeAnything();

    void onBeforeInstantiation();
    void onAfterInstantiation() throws ContradictionException;

    void onBeforeRemoval();
    void onAfterRemoval() throws ContradictionException ;
    default void onAfterSingletonFound(){

    }
    default void onAfterInstantiationPropagation() throws ContradictionException {}

    default void buildBranch(){}

    void baseState();
}
