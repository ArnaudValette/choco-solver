/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;

public interface ISingletonConsistencyStrategy {

   void propagate(SingletonConsistencyEngine engine) throws ContradictionException;

    void basePropagation() throws ContradictionException;
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
    default boolean _isNeighborhoodAlgo(){
      return false;
    }
}
