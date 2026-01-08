/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.propagation.examples;

import org.chocosolver.memory.IEnvironment;
import org.chocosolver.sat.MiniSat;
import org.chocosolver.solver.*;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.propagation.PropagationEngine;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;

import java.util.*;

public class SACEngine extends PropagationEngine implements ICause {

    Model model;
    HashMap<Integer, List<Propagator<?>>> propmap;
    ArrayDeque<Pair<IntVar, Integer>> pendingList;
    IEnvironment env;
    Solver solver;
    double P;

    public SACEngine(Model model, MiniSat sat, double probability) {
        super(model, sat);
        this.model = model;
        propmap = new HashMap<>();
        pendingList = new ArrayDeque<>();
        env = model.getEnvironment();
        solver = model.getSolver();
        P = probability;
    }

    public SACEngine(Model model) {
        this(model, null, 1.0);
    }

    @Override
    public void initialize() throws SolverException {
        super.initialize();
        List<Propagator<?>> p = propagators;
        for (Propagator<?> prop : p) {
            Variable[] vs = prop.getVars();
            for (Variable v : vs) {
                if (!propmap.containsKey(v.getId())) {
                    propmap.put(v.getId(), new ArrayList<>());
                }
                propmap.get(v.getId()).add(prop);
            }
        }
        pendingList = new ArrayDeque<>();
        IntVar[] vars = model.retrieveIntVars(false);
        for (IntVar v : vars) {
            for (int val = v.getLB(); val <= v.getUB(); val = v.nextValue(val)) {
                pendingList.add(new Pair<>(v, val));
            }
        }
    }

    @Override
    public void propagate() throws ContradictionException {
        if (Math.random() <= P) {
            super.propagate();
            while (!pendingList.isEmpty()) {
                Pair<IntVar, Integer> p = pendingList.pop();
                IntVar v = p.getA();
                Integer val = p.getB();
                model.getEnvironment().worldPush();
                Decision<?> d = model.getSolver().getDecisionPath().makeIntDecision(v, DecisionOperatorFactory.makeIntEq(), val);
                d.buildNext();
                try {
                    d.apply();
                    super.propagate();
                    model.getEnvironment().worldPop();
                } catch (ContradictionException e) {
                    model.getEnvironment().worldPop();
                    super.flush();
                    v.removeValue(val, this);
                    super.propagate();
                }
            }
        } else {
            super.propagate();
        }
    }


}