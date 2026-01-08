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
import org.jgrapht.alg.util.Triple;

import java.util.*;
import java.util.stream.Collectors;

public class RNSQEngine extends PropagationEngine {

    boolean checkSingleton = false;
    boolean singleton = false;
    boolean blockLateScheduling = true;
    // boolean restrictToNeighbors = false;
    /* Blacklists,
     * directPropsSchedulingBlacklist: lancés en priorité
     * latePropsSchedulingBlacklist: planifiés "en retard"
     *  */
    BitSet directPropsSchedulingBlacklist = new BitSet();
    BitSet latePropsSchedulingBlacklist = new BitSet();
    // BitSet neighborsWhitelist = new BitSet();

    /* Blacklists maps,
     * 1. Pour empêcher tout propagateur d'être planifié.
     * 2. Pour empêcher les propagateurs qui ne concernent pas directement Xi d'être planifiés.
     * 3. Pour empêcher les propagateurs externes au NSAC de Xi d'être planifiés.
     * */
    HashMap<IntVar, BitSet> BLOCK_SCHEDULING = new HashMap<>();
    HashMap<IntVar, BitSet> SCHEDULE_DIRECT_ONLY = new HashMap<>();
    HashMap<IntVar, BitSet> SCHEDULE_NSAC = new HashMap<>();

    /*
     * voisinage de Xi
     * */
    HashMap<IntVar, Set<IntVar>> neighborhood = new HashMap<>();
    // HashMap<IntVar, BitSet> RESTRICT_TO_NX = new HashMap<>();

    /* Tous les propagateurs */
    List<Propagator<? extends Variable>> props = new ArrayList<>();

    /* PendingLists */
    ArrayDeque<IntVar> pendingList = new ArrayDeque<>();
    ArrayDeque<IntVar> defaultPL = new ArrayDeque<>();

    /* Late Propagators */
    ArrayDeque<Triple<Propagator<?>, Integer, Integer>> latePropagatorsQueue = new ArrayDeque<>();

    Model model;
    Solver solver;
    IEnvironment env;

    boolean change;


    public RNSQEngine(Model model, MiniSat sat) {
        super(model,sat);
        this.model = model;
        env = model.getEnvironment();
        solver = model.getSolver();
        IntVar[] vars = model.retrieveIntVars(true);
        Collections.addAll(defaultPL, vars);
    }

    public RNSQEngine(Model model) {
        this(model, null);
    }


    @Override
    public void initialize() throws SolverException {
        super.initialize();
        props = propagators;

        pendingList = defaultPL.clone();

        /*
         * Pour chaque variable, blacklister tous les propagateurs (par défaut)
         * */

        for (IntVar v : model.retrieveIntVars(true)) {
            SCHEDULE_NSAC.put(v, new BitSet());
            SCHEDULE_DIRECT_ONLY.put(v, new BitSet());
            BLOCK_SCHEDULING.put(v, new BitSet());
            for (Propagator<?> prop : props) {
                SCHEDULE_NSAC.get(v).set(prop.hashCode());
                SCHEDULE_DIRECT_ONLY.get(v).set(prop.hashCode());
                BLOCK_SCHEDULING.get(v).set(prop.hashCode());

            }
        }

        for (Propagator<?> p : props) {
            /*
             * Pour chaque propagateur,
             * l'ôter de la blacklist des variables qu'il concerne
             * */
            for (Variable v : p.getVars()) {

                /* P concerne Xi */
                SCHEDULE_DIRECT_ONLY.get(v).clear(p.hashCode());
                /* P est dans NSAC de N(Xi) */
                SCHEDULE_NSAC.get(v).clear(p.hashCode());

                if (!neighborhood.containsKey(v)) {
                    /* HM init: V */
                    neighborhood.put((IntVar) v, new HashSet<>());
                    //RESTRICT_TO_NX.put((IntVar) v, new BitSet());
                }

                /*
                 * Pour chaque variable, trouver ses voisines
                 * */
                for (Variable u : p.getVars()) {
                    /* U et V sont voisines par P */
                    //if (!neighborhood.containsKey(u)) {
                        //neighborhood.put((IntVar) u, new HashSet<>());
                        //RESTRICT_TO_NX.put((IntVar) u, new BitSet());
                    //}
                    //neighborhood.get(u).add((IntVar) v);
                    if (!u.equals(v)) {
                        neighborhood.get(v).add((IntVar) u);
                        //neighborhood.get(u).add((IntVar) v);
                        //RESTRICT_TO_NX.get((IntVar) v).set(u.getId());
                        //RESTRICT_TO_NX.get((IntVar) u).set(v.getId());
                    }
                }
            }
            /*
             * \og P concerne Xi \fg n'est pas une condition suffisante pour trouver tous les propagateurs
             * qui sont dans le NSAC de N(Xi).
             *
             * The problem PN = (XN U {Xi}, CN ) is arc consistent, where XN is the neighbour-
             * hood of Xi and CN is the set of all constraints whose scope
             * includes at least two members of the set XN U {Xi}.
             *
             * https://cdn.aaai.org/ocs/12807/12807-57630-1-PB.pdf
             *
             * Nous travaillons sur blacklistMapIndirect en vue d'autoriser ces contraintes dont le scope
             * concerne au moins deux membres de XN U {Xi} sans nécessairement concerner Xi (traité au dessus).
             *
             * Autrement dit, il nous faut rajouter les contraintes dont le scope concerne au moins deux membres
             * de XN \ {Xi}.
             * */
        }

        for (IntVar v : model.retrieveIntVars(true)) {
            Set<IntVar> nv = neighborhood.get(v);
            if (nv != null) {
                for (Propagator<?> p : props) {
                    Set<Variable> pv = Arrays.stream(p.getVars()).collect(Collectors.toSet());
                    if (nv.stream().filter(pv::contains).count() >= 2) {
                        SCHEDULE_NSAC.get(v).clear(p.hashCode());
                    }
                }
            }
        }
        change = false;
    }

    @Override
    public boolean isInitialized() {
        return super.isInitialized();
    }

    private void ACenforce() throws ContradictionException {
        checkSingleton = false;
        blockLateScheduling = true;
        super.propagate();
    }

    private void conditionFC() throws ContradictionException {
        blockLateScheduling = false;
        singleton = false;
        checkSingleton = true;
        super.propagate();
    }

    private void neighborhoodAC() throws ContradictionException {
        blockLateScheduling=true;
        //restrictToNeighbors=true;
        checkSingleton=false;
        super.propagate();
    }

    @Override
    public void propagate() throws ContradictionException {
        pendingList=defaultPL.clone();
        directPropsSchedulingBlacklist= new BitSet();
        ACenforce();

        while(!pendingList.isEmpty()){

            boolean changed = false;
            IntVar v = pendingList.pop();
            BitSet direct_only = SCHEDULE_DIRECT_ONLY.get(v);
            BitSet nsac_props = SCHEDULE_NSAC.get(v);
            //neighborsWhitelist = RESTRICT_TO_NX.get(v);

            Set<IntVar> nx = neighborhood.get(v);

            for(int val = v.getLB(); val<=v.getUB(); val=v.nextValue(val)) {
                try {
                    env.worldPush();
                    IntDecision d = decide(v, val);
                    d.buildNext();
                    d.apply();

                    /* apply condition FC */

                    latePropagatorsQueue = new ArrayDeque<>();
                    directPropsSchedulingBlacklist = direct_only;
                    latePropsSchedulingBlacklist = nsac_props;

                    conditionFC();

                    if(singleton){
                        singleton=false;
                        directPropsSchedulingBlacklist =nsac_props;

                        while(!latePropagatorsQueue.isEmpty()){
                            Triple<Propagator<?>, Integer, Integer> args = latePropagatorsQueue.pop();
                            super.schedule(args.getFirst(), args.getSecond(), args.getThird());
                        }
                        /* apply AC to N(v) */
                        neighborhoodAC();
                    }

                    env.worldPop();
                    flush();

                } catch (ContradictionException e) {
                    /* DOM-WIPEOUT */
                    env.worldPop();
                    flush();
                    v.removeValue(val, Cause.Null);
                    changed=true;
                }
            }
            if(changed){
                /* add neighbors to the queue */
                for(IntVar u : nx){
                    if(!pendingList.contains(u)) {
                        pendingList.add(u);
                    }
                }
                // pendingList.add(v);
            }
        }
    }

    private IntDecision decide(IntVar x, Integer val){
        return model.getSolver().getDecisionPath().makeIntDecision(x, DecisionOperatorFactory.makeIntEq(), val);
    }

    public void onSingleton(Variable v){
        if(checkSingleton) {
            if (v.getDomainSize() == 1) {
                singleton = true;
                checkSingleton = false;
            }
        }
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
        onSingleton(variable);
        super.onVariableUpdate(variable, type, cause);
    }

    @Override
    public void schedule(Propagator<?> prop, int pindice, int mask){
        if (!directPropsSchedulingBlacklist.get(prop.hashCode())) {
            super.schedule(prop, pindice, mask);
        }
        else if(!blockLateScheduling && !latePropsSchedulingBlacklist.get(prop.hashCode())){
            latePropagatorsQueue.add(new Triple<>(prop,pindice,mask));
        }
    }

}
