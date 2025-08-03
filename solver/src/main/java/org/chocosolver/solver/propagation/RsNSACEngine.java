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
import java.util.stream.Collectors;

public class RsNSACEngine extends NSACEngine implements IVariableMonitor<IntVar> {

    private final Set<Propagator<IntVar>> maxScopeProps;

    public RsNSACEngine(Model model, MiniSat sat) {
        super(model, sat);
        maxScopeProps = new HashSet<>();
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

        propagationScope.addAll(closeNeighbourProps.get(var));
        maxScopeProps.addAll(closeNeighbourProps.get(var));

        var.instantiateTo(val, Cause.Null);
        basePropagation();
    }
    @Override
    public void onUpdate(IntVar var, IEventType evt) throws ContradictionException {
        if (evt == IntEventType.INSTANTIATE){
            propagationScope.addAll(
                    maxScopeProps.stream()
                            .filter(closeNeighbourProps.get(var)::contains)
                            .collect(Collectors.toSet())
            );
        }
    }
}
