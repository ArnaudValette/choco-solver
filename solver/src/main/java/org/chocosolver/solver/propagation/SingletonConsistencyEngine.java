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

    /** @boolean
     *  Indicates if this engine must filter the propagators scheduling operation,
     *  this may be what you need when implementing specific algorithms (e.g. RNSAC, RsNSAC).
     *  When set to true, the BitSets {@link SingletonConsistencyEngine#directPropsSchedulingBlacklist}
     *  and {@link SingletonConsistencyEngine#latePropsSchedulingBlacklist}
     *  are used as filters in {@link SingletonConsistencyEngine#schedule(Propagator, int, int)}
     * */
    boolean doesFilterPropagationScheduling = false;

    /** @boolean
     * Indicates if this engine must monitor the domain of variables
     * that are modified during propagation to spot singleton domains.
     */
    boolean checkSingleton = false;

    /** @boolean
     * Indicates that the propagation has reduced some domains to singletons.
     * */
    boolean singleton = false;

    /** @boolean
     * Indicates if this engine must stop filling the {@link SingletonConsistencyEngine#latePropagatorsQueue}
     * during propagation.
     * */
    boolean blockLateScheduling = true;

    /** @boolean
     * Indicates if this engine must fill the {@link SingletonConsistencyEngine#subNeighborhoodBlacklist}
     * during propagation. */
    boolean collectSingleton = false;

    /** @BitSet
     * Every blacklisted propagators from this {@link BitSet}
     * won't be directly scheduled by this engine if {@link SingletonConsistencyEngine#doesFilterPropagationScheduling}
     * is true. */
    BitSet directPropsSchedulingBlacklist = new BitSet();

    /** @BitSet
     * Propagators that aren't blacklisted by this {@link BitSet}
     * will be stored in {@link SingletonConsistencyEngine#latePropagatorsQueue} if
     * they aren't whitelisted by {@link SingletonConsistencyEngine#directPropsSchedulingBlacklist},
     * {@link SingletonConsistencyEngine#doesFilterPropagationScheduling} is true and
     * {@link SingletonConsistencyEngine#blockLateScheduling} is false.*/
    BitSet latePropsSchedulingBlacklist = new BitSet();

    /** @BitSet
     * Blacklist propagators that are not in the sub-neighborhood of Xi according to
     * RsNSAC. (Available after a propagation where {@link SingletonConsistencyEngine#checkSingleton}
     * and {@link SingletonConsistencyEngine#collectSingleton} were true before propagating and
     * {@link SingletonConsistencyEngine#singleton} is true after the operation.
     * */
    BitSet subNeighborhoodBlacklist = new BitSet();

    HashMap<IntVar, BitSet> BLOCK_SCHEDULING = new HashMap<>();
    HashMap<IntVar, BitSet> SCHEDULE_DIRECT_ONLY = new HashMap<>();
    HashMap<IntVar, BitSet> SCHEDULE_NSAC = new HashMap<>();
    BitSet defaultSubNeighborhood = new BitSet();

    HashMap<IntVar, Set<IntVar>> neighborhood = new HashMap<>();
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

    public SingletonConsistencyEngine enforceRsNSQ(){
        propagationStrategy = (new RNSQDriverStrategy(PL)).restrictToSubNeighborhood(this);
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
            /* Initialiser subNeighborhood : all set */
            defaultSubNeighborhood.set(p.hashCode());

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
            if(collectSingleton){
                updateSubNeighborhoodBlacklist(v);
            }
            else {
                checkSingleton = false;
            }
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

    public void freeLatePropsSchedulingBL(){
        latePropsSchedulingBlacklist = new BitSet();
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

    public BitSet getSubNeighborhoodBlacklist(){
        return subNeighborhoodBlacklist;
    }

    public void setLatePropsScheduling(BitSet b){
        latePropsSchedulingBlacklist = b;
    }

    public boolean foundSingletonDuringPropagation(){
        if(singleton){
            singleton = false;
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


    public void setCollectSingleton(boolean collectSingleton) {
        this.collectSingleton = collectSingleton;
    }

    public void reinitSubNeighborhoodBlacklist(){
        this.subNeighborhoodBlacklist = (BitSet) defaultSubNeighborhood.clone();
        /* All propagators are blacklisted */
    }

    public void updateSubNeighborhoodBlacklist(Variable v){
        /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
         * (a) subNeighborhoodBlacklist is initialized with all propagators
         * blacklisted.
         * (b) We will call this method everytime we find a singleton restricted domain
         * when a variable is updated.
         * (c) Each variable comes with a precomputed _direct-only_ blacklist
         * that informs us if a propagator is directly related to that variable (1: not related, 0: related | blacklist).
         * (d) the subNeighborhood blacklist should informs us if a propagator is directly related
         * to a singleton restricted variable (SRV) [1: not related with any SRV, 0: related with at least one SRV].
         * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         * (i.e.) the subNeighborhood blacklist is the AND or INTER of all SRV blacklists.
         * */
        BitSet propList = (BitSet) SCHEDULE_DIRECT_ONLY.get(v).clone();
        subNeighborhoodBlacklist.and(propList);
    }

}
