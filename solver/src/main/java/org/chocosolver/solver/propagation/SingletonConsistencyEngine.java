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
import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.propagation.consistencyStrategy.ISingletonConsistencyStrategy;
import org.chocosolver.solver.propagation.consistencyStrategy.RNSQDriverStrategy;
import org.chocosolver.solver.propagation.consistencyStrategy.SAC3DriverStrategy;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;
import org.jgrapht.alg.util.Triple;

import java.util.*;
import java.util.stream.Collectors;

/*****************************************************************************************************
 * TODO:
 *
 *
 *
 *
 *
 *
 *
 ******************************************************************************************************/

public class SingletonConsistencyEngine extends EngineWrapper implements IPropagationEngine{

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                  ATTRIBUTES
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    boolean doesFilterPropagationScheduling = false;
    boolean checkSingleton = false;
    boolean singleton = false;
    boolean blockLateScheduling = true;

    /* Blacklists,
     * directPropsSchedulingBlacklist: lancés en priorité
     * latePropsSchedulingBlacklist: planifiés "en retard" :
     *   | stockés dans latePropagatorsQueue,
     *   | la stratégie du moteur est en charge
     *   | de décider ce qu'il faut en faire
     *  */
    BitSet directPropsSchedulingBlacklist = new BitSet();
    BitSet latePropsSchedulingBlacklist = new BitSet();

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

    /* Tous les propagateurs */
    List<Propagator<? extends Variable>> props = new ArrayList<>();

    /*
    * PendingLists
    *
    * pendingList: algorithmes basés sur des queues de variables (e.g. RNSQ)
    * instantiationList: algorithmes basés sur des couples (variable, valeur) (e.g. SAC3)
    * TODO: custom reinitialisable weighted queue datastructure with fast contains(x) method
    * */

    ReinitialisableQueue<IntVar> PL = new ReinitialisableQueue<>();
    ReinitialisableQueue<Pair<IntVar, Integer>> IL = new ReinitialisableQueue<>();

    /*
     * Late Propagators
     * * * * * */

    ArrayDeque<Triple<Propagator<?>, Integer, Integer>> latePropagatorsQueue = new ArrayDeque<>();

    ISingletonConsistencyStrategy propagationStrategy;

    int passes;

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                  CONSTRUCTORS
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public SingletonConsistencyEngine(Model model, MiniSat sat){
        super(model, sat);
        propagationStrategy = new RNSQDriverStrategy(PL);
        IntVar[] vars = model.retrieveIntVars(true);

        for (IntVar v : vars) {
            PL.initAdd(v);
            for (int val = v.getLB(); val <= v.getUB(); val = v.nextValue(val)) {
                IL.initAdd(new Pair<>(v, val));
            }
        }
        PL.commitAndLock();
        IL.commitAndLock();
    }

    public SingletonConsistencyEngine(Model model){
        this(model, null);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                  BUILDER
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public SingletonConsistencyEngine enforceRNSQ(){
        propagationStrategy = new RNSQDriverStrategy(PL);
        return this;
    }

    public SingletonConsistencyEngine enforceSAC3(){
        propagationStrategy = new SAC3DriverStrategy(IL);
        return this;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                  Generic
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public SingletonConsistencyEngine setPropagationStrategy(ISingletonConsistencyStrategy s){
        propagationStrategy = s;
        return this;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                  METHODS
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public IntDecision decide(IntVar x, Integer val){
        return model.getSolver().getDecisionPath().makeIntDecision(x, DecisionOperatorFactory.makeIntEq(), val);
    }

    public boolean inModel(IntVar v, int value){
        return v.contains(value);
    }

    @Override
    public void initialize() throws SolverException {
        /* TODO: externalize this */
        pe.initialize();
        props = pe.propagators;

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
                    if (!u.equals(v)) {
                        neighborhood.get(v).add((IntVar) u);
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
    }

    @Override
    public void propagate() throws ContradictionException{
        propagationStrategy.propagate(this);
    }

    public void doPropagate() throws ContradictionException{
        pe.propagate();
    }


    @Override
    public void onVariableUpdate(Variable variable, IEventType type, ICause cause){
        if(checkSingleton) {
            onSingleton(variable);
        }
        pe.onVariableUpdate(variable, type, cause);
    }


    @Override
    public void schedule(Propagator<?> prop, int pindice, int mask){
        /** c.f. {@link RNSQDriverStrategy} && {@link SAC3DriverStrategy}
         * - some consistency techniques require to avoid propagating specific propagators (non-neighbors, ...)
         * - some consistency techniques require to late schedule specific propagators
         * e.g. RNSQ:
         * enforce AC on the whole network,
         * for each (Xi,Vj)
         * instantiate Xi to Vj
         * apply condition FC (propagate only propagators directly related to Xi)
         * if a neighbor of Xi has singleton domain
         * enforce AC on the sub-problem N(Xi)
         * (i.e. include some of the previously blacklisted propagators [i.e. the ones that concern at least two elements of N(Xi)])
         *
         * Here, we use two different blacklists (direct and late) for propagation
         * late propagators are stored in a queue (the driver strategy is in charge of deciding what to do with them)
         * (e.g. you may want to schedule them after having met some specific condition)
        * */
        if(doesFilterPropagationScheduling) {
            if (!directPropsSchedulingBlacklist.get(prop.hashCode())) {
                pe.schedule(prop, pindice, mask);
            } else if (!blockLateScheduling && !latePropsSchedulingBlacklist.get(prop.hashCode())) {
                latePropagatorsQueue.add(new Triple<>(prop, pindice, mask));
            }
        }
        else{
            pe.schedule(prop, pindice, mask);
        }
    }

    @Override
    public void onSingleton(Variable v){
        if (v.getDomainSize() == 1) {
            singleton = true;
            checkSingleton=false;
        }
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                  GETTERS/SETTERS/INTERFACEUTILITIES
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public Triple<Propagator<?>, Integer, Integer> latePropsPop(){
        return latePropagatorsQueue.pop();
    }

    public void freeDirectPropsSchedulingBL(){
        directPropsSchedulingBlacklist = new BitSet();
    }

    public BitSet getDirectOnlyBlacklist(Variable v){
        return SCHEDULE_DIRECT_ONLY.get(v);
    }

    public BitSet getNsacBlacklist(Variable v){
        return SCHEDULE_NSAC.get(v);
    }

    public Set<IntVar> getNeighborhood(Variable v){
        return neighborhood.get(v);
    }

    public void worldPush(){
        env.worldPush();
    }

    public void worldPopNFlush(){
        env.worldPop();
        flush();
    }

    public int getWorldIndex(){
        return env.getWorldIndex();
    }

    public void worldPopUntilNFlush(int id){
        env.worldPopUntil(id);
        flush();
    }

    public void initLatePropQ(){
        latePropagatorsQueue = new ArrayDeque<>();
    }

    public boolean lateQisEmpty(){
        return latePropagatorsQueue.isEmpty();
    }

    public void setDirectPropsScheduling(BitSet b){
        directPropsSchedulingBlacklist=b;
    }

    public void setLatePropsScheduling(BitSet b){
        latePropsSchedulingBlacklist = b;
    }

    public boolean foundSingletonDuringPropagation(){
        if(singleton){
            singleton=false;
            return true;
        }
        return false;
    }

    public void imperativeSchedule(Propagator<?> prop, int pindice, int mask){
        /* You may need to escape filters/blacklists temporarily */
        pe.schedule(prop, pindice, mask);
    }


    public void setCheckSingleton(boolean b){
        checkSingleton = b;
    }

    public void setBlockLateScheduling(boolean b){
        blockLateScheduling = b;
    }

    public void setSingleton(boolean b){singleton=b;}

    public void setDoFilterScheduling(boolean b){
        doesFilterPropagationScheduling=b;
    }
}
