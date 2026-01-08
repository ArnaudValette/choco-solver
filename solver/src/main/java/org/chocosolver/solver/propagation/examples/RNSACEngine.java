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
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.propagation.PropagationEngine;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IEventType;

import java.util.*;

public class RNSACEngine extends PropagationEngine {

    BitSet blacklist;
    HashMap<IntVar, BitSet> bl;
    HashMap<IntVar, Set<IntVar>> varmap;

    List<Propagator<? extends Variable>> props;

    ArrayDeque<IntVar> pendingList;
    ArrayDeque<IntVar> defaultPL;

    Model model;
    Solver solver;
    IEnvironment env;

    boolean change;


    public RNSACEngine(Model model, MiniSat sat){
        super(model,sat);
        /* Utile pour gérer les cas du code dans lesquels
        *  PropagationEngine s'offre lui même à une méthode externe
        * (ex. var_queue.pollFirst().schedulePropagators(this.parent); )
        * Cela nous permet de prévenir ces pertes de contrôle.
        * */
        this.model = model;
        env = model.getEnvironment();
        solver = model.getSolver();
        props = new ArrayList<>();
        varmap = new HashMap<>();
        blacklist = new BitSet();
        bl = new HashMap<>();

        defaultPL = new ArrayDeque<>();
        IntVar[] vars = model.retrieveIntVars(true);
        Collections.addAll(defaultPL, vars);
        pendingList=defaultPL.clone();
    }

    public RNSACEngine(Model model){
        this(model, null);
    }


    @Override
    public void initialize() throws SolverException {
        super.initialize();
        props = propagators;
        for(Propagator<?> p  : props){
            /* Pour chaque propagateur */
            for(Variable v : p.getVars()){
                /* Pour chacune de ses variables */
                if(!bl.containsKey((IntVar) v)){
                    bl.put((IntVar) v, new BitSet());
                    for(Propagator<?> pp : props){
                        bl.get(v).set(pp.hashCode());
                    }
                }
                bl.get(v).clear(p.hashCode());
                if(!varmap.containsKey(v)){
                    varmap.put((IntVar) v, new HashSet<>());
                }
                for(Variable u : p.getVars()){
                    if(!u.equals(v)){
                        varmap.get(v).add((IntVar) u);
                    }
                }
            }
        }
        change=false;
    }

    @Override
    public boolean isInitialized(){
        return super.isInitialized();
    }

    @Override
    public void propagate() throws ContradictionException {
        pendingList=defaultPL.clone();
        super.propagate();
        while(!pendingList.isEmpty()){
            boolean changed = false;
            IntVar v = pendingList.pop();
            blacklist = bl.get(v);
            for(int val = v.getLB(); val<=v.getUB(); val=v.nextValue(val)) {
                int idx = env.getWorldIndex();
                try {
                    env.worldPush();
                    IntDecision d = decide(v, val);
                    d.buildNext();
                    d.apply();
                    super.propagate();
                    env.worldPopUntil(idx);
                    flush();
                } catch (ContradictionException ignore) {
                    changed = true;
                    env.worldPopUntil(idx);
                    flush();
                    v.removeValue(val, Cause.Null);
                }
            }
            if(changed){
                for(IntVar u : varmap.get(v)){
                    if(!pendingList.contains(u)){
                        pendingList.add(u);
                    }
                }
            }
        }
        blacklist = new BitSet(); // reinitialize the blacklist
    }

    private IntDecision decide(IntVar x, Integer val){
        return model.getSolver().getDecisionPath().makeIntDecision(x, DecisionOperatorFactory.makeIntEq(), val);
    }

    @Override
    public void execute(Propagator<?> propagator)throws ContradictionException{
        super.execute(propagator);
    }

    @Override
    public void flush(){
        super.flush();
    }

    @Override
    public void onVariableUpdate(Variable variable, IEventType type, ICause cause){
        super.onVariableUpdate(variable, type, cause);
    }

    @Override
    public void schedule(Propagator<?> prop, int pindice, int mask){
        if (!blacklist.get(prop.hashCode())) {
            super.schedule(prop, pindice, mask);
        }
    }
}
