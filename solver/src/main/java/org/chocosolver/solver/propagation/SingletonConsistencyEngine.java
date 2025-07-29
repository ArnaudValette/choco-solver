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
import org.chocosolver.solver.propagation.consistencyStrategy.types.PairBasedStrategy;
import org.chocosolver.solver.propagation.consistencyStrategy.types.VariableBasedStrategy;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;
import org.jgrapht.alg.util.Triple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

public class SingletonConsistencyEngine extends PropagationEngine implements ICause{

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

    HashMap<IntVar, Set<Pair<IntVar, Integer>>> neighborhood = new HashMap<>();
    private HashMap<IntVar, Set<IntVar>> _neighborhood = new HashMap<>();
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


    int passes=0;
    int currentPass=0;
    boolean doConsumePasses =false;
    BitSet passBlacklist = new BitSet();
    ArrayList<Triple<Propagator<?>, Integer, Integer>> passPropagatorsList = new ArrayList<>();

    public IntVar[] getVars(){
        return model.retrieveIntVars(true);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                  CONSTRUCTORS
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public SingletonConsistencyEngine(Model model, MiniSat sat){
        super(model, sat);

        PL.setSupplier((_void)-> Arrays.stream(model.retrieveIntVars(true)).collect(Collectors.toCollection(ArrayDeque::new)));
        IL.setSupplier((_void) ->
                Arrays.stream(model.retrieveIntVars(true))
                        .flatMap(v -> IntStream.iterate(v.getLB(), val -> val <= v.getUB(), v::nextValue)
                        .mapToObj(val -> new Pair<>(v, val)))
                        .collect(Collectors.toCollection(ArrayDeque::new)));

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


    public SingletonConsistencyEngine onePass(){
        passes = 1;
        return this;
    }

    public SingletonConsistencyEngine twoPasses(){
        passes = 2;
        return this;
    }

    public SingletonConsistencyEngine threePasses(){
        passes = 3;
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

    public SingletonConsistencyEngine setPropagationStrategy(PairBasedStrategy s){
        propagationStrategy = s;
        s.setQ(IL);
        return this;
    }

    public SingletonConsistencyEngine setPropagationStrategy(VariableBasedStrategy s){
        propagationStrategy = s;
        s.setQ(PL);
        return this;
    }

    public SingletonConsistencyEngine setPasses(int p){
        passes = p;
        return this;
    }

    public void provideQ(VariableBasedStrategy s){
        s.setQ(PL);
    }

    public void provideQ(PairBasedStrategy s){
        s.setQ(IL);
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
        super.initialize();
        props = propagators;

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
                    _neighborhood.put((IntVar) v, new HashSet<>());
                    //RESTRICT_TO_NX.put((IntVar) v, new BitSet());
                }

                /*
                 * Pour chaque variable, trouver ses voisines
                 * */
                for (Variable u : p.getVars()) {
                    /* U et V sont voisines par P */
                    if (!u.equals(v)) {
                        IntVar variable = (IntVar) u;
                        for(int value = variable.getLB(); value <= variable.getUB(); value = variable.nextValue(value)) {
                            neighborhood.get(v).add(new Pair<>(variable, value));
                        }
                        _neighborhood.get(v).add(variable);
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
            Set<IntVar> nv = _neighborhood.get(v);
            if (nv != null) {
                for (Propagator<?> p : props) {
                    Set<Variable> propSet = Arrays.stream(p.getVars()).collect(Collectors.toSet());
                    int matches = 0;
                    for(Variable propVar : propSet){
                        if(nv.contains(propVar)){
                            matches++;
                        }
                        if(matches >= 2){
                            SCHEDULE_NSAC.get(v).clear(p.hashCode());
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void propagate() throws ContradictionException{
        /* The strategy is in charge of setting this */
        doConsumePasses=false;
        propagationStrategy.propagate(this);
    }

    public void doPropagate() throws ContradictionException{
        super.propagate();
    }


    @Override
    public void onVariableUpdate(Variable variable, IEventType type, ICause cause){
        if(checkSingleton) {
            onSingleton(variable);
        }
        super.onVariableUpdate(variable, type, cause);
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
                doSchedule(prop, pindice, mask);
            } else if (!blockLateScheduling && !latePropsSchedulingBlacklist.get(prop.hashCode())) {
                latePropagatorsQueue.add(new Triple<>(prop, pindice, mask));
            }
        }
        else{
            doSchedule(prop, pindice, mask);
        }
    }

    public void doSchedule(Propagator<?> prop, int pindice, int mask){
        if(doConsumePasses) {
            if(!passBlacklist.get(prop.hashCode())) {
                super.schedule(prop, pindice, mask);
                passBlacklist.set(prop.hashCode());
                passPropagatorsList.add(new Triple<>(prop, pindice, mask));
            }
        }
        else{
            super.schedule(prop, pindice, mask);
        }
    }

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

    public Triple<Propagator<?>, Integer, Integer> passQueueAt(int index){
        return passPropagatorsList.get(index);
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

    public Set<Pair<IntVar, Integer>> getNeighborhoodAsPairs(Variable v){
        return neighborhood.get(v);
    }

    public Set<IntVar> getNeighborhood(Variable v){
        return _neighborhood.get(v);
    }

    public void worldPush(){
        model.getEnvironment().worldPush();
    }

    public void worldPop(){
        model.getEnvironment().worldPop();
    }

    public void worldPopNFlush(){
        flush();
        model.getEnvironment().worldPop();
    }

    public int getWorldIndex(){
        return model.getEnvironment().getWorldIndex();
    }

    public void worldPopUntilNFlush(int id){
        flush();
        model.getEnvironment().worldPopUntil(id);
    }

    public void initLatePropQ(){
        latePropagatorsQueue = new ArrayDeque<>();
    }

    public void initPassPropList(){
        passPropagatorsList = new ArrayList<>();
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
        super.schedule(prop, pindice, mask);
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

    public void setDoConsumePasses(boolean doConsumePasses) {
        this.doConsumePasses = doConsumePasses;
    }

    public void reinitPassBlacklist(){
        passBlacklist.clear();
    }

    public void passesInit(){
        currentPass = 0;
    }
    public void passesIncrement(){
        currentPass++;
    }

    public boolean hasPasses(){
        return currentPass < passes;
    }

    public int passPropagatorsSize(){
        return passPropagatorsList.size();
    }
}
