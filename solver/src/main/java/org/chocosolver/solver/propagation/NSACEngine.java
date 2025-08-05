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
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.stream.Collectors;

public class NSACEngine extends SACEngine {
    protected final Map<IntVar, Set<Propagator<IntVar>>> closeNeighbourProps;
    protected final Map<IntVar, Set<Propagator<IntVar>>> neighbourProps;

    protected final Set<Propagator<IntVar>> propagationScope;

    protected boolean useReducedScope;

    public NSACEngine(Model model, MiniSat sat) {
        super(model, sat);
        int initSize = model.retrieveIntVars(true).length;
        closeNeighbourProps = new HashMap<>(initSize);
        neighbourProps = new HashMap<>(initSize);
        propagationScope = new HashSet<>();
    }


    @Override
    public void initialize() throws SolverException {
        super.initialize();
        Map<IntVar, Set<IntVar>> neighbourVars = new HashMap<>();
        for(IntVar var : model.retrieveIntVars(true)){
            closeNeighbourProps.put(var, new HashSet<>());
            neighbourProps.put(var, new HashSet<>());
            neighbourVars.put(var, new HashSet<>());
        }

        List<Propagator<IntVar>> propagators = Arrays.stream(model.getCstrs())
                .flatMap(cstr -> Arrays.stream(cstr.getPropagators()))
                .filter(prop -> Arrays.stream(prop.getVars()).allMatch(IntVar.class::isInstance))
                .map(prop -> (Propagator<IntVar>) prop)
                .collect(Collectors.toList());

        for(Propagator<IntVar> propagator : propagators){
            List<IntVar> scope = Arrays.asList(propagator.getVars());
            for(IntVar var : scope){
                closeNeighbourProps.get(var).add(propagator);
                neighbourProps.get(var).add(propagator);
                neighbourVars.get(var).addAll(scope);
            }
        }

        for(IntVar var : model.retrieveIntVars(true)){
            for(IntVar neighbour: neighbourVars.get(var)){
                for(Propagator<IntVar> neighProp : closeNeighbourProps.get(neighbour)){
                    Set<IntVar> scope = new HashSet<>(Arrays.asList(neighProp.getVars()));
                    scope.retainAll(neighbourVars.get(var));
                    if(scope.size()>=3) { // var + neighbour + an other var
                        neighbourProps.get(var).add(neighProp);
                    }
                }
            }
        }
    }

    @Override
    public void propagate() throws ContradictionException {
        useReducedScope = false;
        basePropagation();
        useReducedScope = true;
        singletonArcConsistency();
        propagationScope.clear();
    }

    @Override
    protected void hasSupport(IntVar var, int val) throws ContradictionException {
        propagationScope.clear();
        propagationScope.addAll(neighbourProps.get(var));
        var.instantiateTo(val, Cause.Null);
        basePropagation();
    }

    @Override
    public void schedule(Propagator<?> prop, int pindice, int mask) {
        if (!useReducedScope || propagationScope.contains(prop)){
            prop.doScheduleEvent(pindice, mask);
            notEmpty |= (1 << prop.doSchedule(pro_queue));
        }
    }
}
