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
import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.propagation.PropagationEngine;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;

public class SAC3Engine extends PropagationEngine {

    PropagationEngine pe;
    Model model;
    ArrayDeque<Pair<IntVar, Integer>> instantiationList;
    ArrayDeque<Pair<IntVar, Integer>> defaultIL = new ArrayDeque<>();
    IEnvironment env;
    Solver solver;
    boolean change;

    public SAC3Engine(Model model, MiniSat sat) {
        super(model,sat);
        this.hybrid = 0b10;
        this.model = model;
        instantiationList = new ArrayDeque<>();
        env = model.getEnvironment();
        solver = model.getSolver();
        IntVar[] vars = model.retrieveIntVars(false);
        for (IntVar v : vars) {
            for (int val = v.getLB(); val <= v.getUB(); val = v.nextValue(val)) {
                defaultIL.add(new Pair<>(v, val));
            }
        }
    }

    public SAC3Engine(Model model) {
        this(model, null);
    }

    @Override
    public void initialize() throws SolverException {
        super.initialize();
        change = false;

    }

    @Override
    public void propagate() throws ContradictionException{
        super.propagate();
        change = false;
        buildPendingList();
        while (!instantiationList.isEmpty()) {
            Pair<IntVar, Integer> p = instantiationList.pop();
            IntVar v = p.getA();
            Integer val = p.getB();
            if (inModel(v, val)) {
                if (!buildBranch(decide(v, val))) {
                    v.removeValue(val, Cause.Null); // Throw means fail
                    super.propagate(); // Throw means fail
                    change = true;
                }
            }
            if (instantiationList.isEmpty() && change) {
                buildPendingList();
                change = false;
            }
        }
    }

    private boolean buildBranch(Decision<?> d) {
        int id = env.getWorldIndex();
        try {
            env.worldPush();
            d.buildNext();
            d.apply();
            super.propagate();
        } catch (ContradictionException i) {
            env.worldPopUntil(id);
            super.flush();
            return false;
        }
        int t = instantiationList.size();
        int i = 0;
        while(i<t) {
            i++;
            /* Select a value (Xj, b) from PendingList Inter D, remove it from PendingList */
            Pair<IntVar, Integer> p = instantiationList.pop();
            if(!inModel(p.getA(),p.getB())) {
                /* i.e. if value not in D, don't remove it from PendingList */
                instantiationList.add(p);
                continue;
            }
            IntVar v = p.getA();
            Integer value = p.getB();
                try {
                    env.worldPush();
                    IntDecision d2 = decide(v, value);
                    d2.buildNext();
                    d2.apply();
                    super.propagate();
                } catch (ContradictionException ignore) {
                    instantiationList.add(p);
                    break;
                }
            }
        env.worldPopUntil(id);
        super.flush();
        return true;
    }


    private boolean inModel(IntVar v, int val){
        return v.contains(val);
    }

    private void buildPendingList(){
        instantiationList = defaultIL.clone();
    }

    private IntDecision decide(IntVar x, Integer val){
        return model.getSolver().getDecisionPath().makeIntDecision(x, DecisionOperatorFactory.makeIntEq(), val);
    }
}
