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
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.IVariableMonitor;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.IntEventType;

import java.util.HashSet;
import java.util.Set;

public class RNSACEngine extends NSACEngine implements IVariableMonitor<IntVar> {
    private final Set<Propagator<IntVar>> maxScopeProps;
    private final Set<Propagator<IntVar>> waitingRoom;

    private  boolean isRestricted;

    public RNSACEngine(Model model, MiniSat sat) {
        super(model, sat);
        maxScopeProps = new HashSet<>();
        waitingRoom = new HashSet<>();
    }

    @Override
    public void initialize() throws SolverException {
        super.initialize();
        for(IntVar var: model.retrieveIntVars(true)){
            var.addMonitor(this);
        }
    }

    @Override
    protected void hasSupport(IntVar var, int val) throws ContradictionException {
        propagationScope.clear();
        maxScopeProps.clear();
        isRestricted = true;

        propagationScope.addAll(closeNeighbourProps.get(var));
        maxScopeProps.addAll(closeNeighbourProps.get(var));

        var.instantiateTo(val, Cause.Null);
        basePropagation();
    }

    @Override
    public void onUpdate(IntVar var, IEventType evt) {
        if (isRestricted && evt == IntEventType.INSTANTIATE){
            propagationScope.addAll(maxScopeProps);
            for(Propagator<IntVar> prop : waitingRoom){
                schedule(prop);
            }
            isRestricted = false;
        }
    }

    @Override
    public void schedule(Propagator<?> prop, int pindice, int mask) {
        if (!useReducedScope || propagationScope.contains(prop)){
            prop.doScheduleEvent(pindice, mask);
            notEmpty |= (1 << prop.doSchedule(pro_queue));
        } else if (maxScopeProps.contains(prop)) {
            waitingRoom.add((Propagator<IntVar>) prop);
        }
    }

    public void schedule(Propagator<?> prop) {
        if (!useReducedScope || propagationScope.contains(prop)){
            notEmpty |= (1 << prop.doSchedule(pro_queue));
        } else if (maxScopeProps.contains(prop)) {
            waitingRoom.add((Propagator<IntVar>) prop);
        }
    }
}
