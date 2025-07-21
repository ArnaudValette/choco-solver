/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.propagation;

import org.chocosolver.sat.MiniSat;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.PropagatorEventType;

import java.util.Collection;

public interface IPropagationEngine {
    public void initialize();

    public boolean isInitialized();

    public void propagate() throws ContradictionException ;

    public void execute(Propagator<?> propagator)throws ContradictionException;

    public void flush();

    public void onVariableUpdate(Variable variable, IEventType type, ICause cause);


    public void schedule(Propagator<?> prop, int pindice, int mask);

    default public void onSingleton(Variable v){}
    default public void onPass(){}

    public void delayedPropagation(Propagator<?> propagator, PropagatorEventType type);

    public int getDelayedPropagation();

    public void onPropagatorExecution(Propagator<?> propagator);

    public void deactivatePropagator(Propagator<?> propagator);

    public void setInsight(PropagationInsight insight);

    public void setHybrid(byte hybrid);

    public void reset();

    public void clear();

    public void ignoreModifications();

    public void dynamicAddition(boolean permanent, Propagator<?>... ps) throws SolverException;

    public void updateInvolvedVariables(Propagator<?> p);

    public void propagateOnBacktrack(Propagator<?> propagator);

    public void dynamicDeletion(Propagator<?>...ps);

}
