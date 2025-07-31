package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.variables.IntVar;
import org.jgrapht.alg.util.Triple;

public abstract class AbstractSingletonStrategy<T> extends BaseStrategy<T> implements ICause {
    protected SingletonConsistencyEngine E;
    protected IntVar Xi;
    protected Integer Aj;
    protected boolean willConsumePasses = false;
    protected int lastId;

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                                         Core Method
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Override
    public void propagate(SingletonConsistencyEngine engine) throws ContradictionException {
        if(Q == null){
            engine.provideQ(this);
        }
        changed = false;
        E=engine;
        onBeforeAnything();
        E.doPropagate();
        Q.reinit();
        loop();
    }

    /** The general shape of SAC based algorithms */
    protected void task() throws ContradictionException {
        if(Xi.contains(Aj)) {
            lastId = E.getWorldIndex();
            E.worldPush();
            try {
                onBeforeInstantiation(); /* e.g. set a blacklist to prevent non-neighborhood propagation (NSAC) */
                instantiate(Xi, Aj);
                onAfterInstantiation(); /* e.g. E.doPropagate() (SAC) */
            } catch (ContradictionException ce) {
                onBeforeRemoval();
                remove(Xi,Aj);
                onAfterRemoval(); /* e.g. E.doPropagate() */
            }
        }
        changed = queueHandler(changed);
    }


    protected boolean queueHandler(boolean changed){
        if(Q.isEmpty() && changed){
            Q.reinit();
            return false;
        }
        return changed;
    }

    protected void instantiate(IntVar X, int a) throws ContradictionException{
        X.instantiateTo(a, this);
    }

    protected  void remove(IntVar X, int a) throws ContradictionException{
        X.removeValue(a, this);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                                         Hooks/Listeners
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    protected void onBeforeAnything(){ baseState(); };

    protected void onBeforeInstantiation(){ baseState(); };

    protected void onAfterInstantiation() throws ContradictionException {
        /* base case : just propagate*/
        E.doPropagate();
        E.worldPopUntilNFlush(lastId);
        baseState();
    };

    protected void onBeforeRemoval(){
        E.worldPopUntilNFlush(lastId);
        baseState();
    };

    protected void onAfterRemoval() throws ContradictionException {
        /* base case : just propagate */
        E.doPropagate();
        changed = true;
        E.flush();
        baseState();
    };

    protected void baseState(){
        E.setDoFilterScheduling(false);
        E.setCheckSingleton(false);
        E.setBlockLateScheduling(true);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                                         Passes Management
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /** @void The method used to consume passes in k-pXXX variants of our consistencies (e.g. simply Engine.doPropagate()
     * in the case of simpler algorithms such as 1-pSAC1). */
    protected abstract void passConsumer() throws ContradictionException;


    public AbstractSingletonStrategy<T> setWillConsumePasses(boolean b){
        willConsumePasses =b;
        return this;
    }

    /* These methods are for enforcing k-passes AC.
    *  We must distinguish between a-pSAC and Sb-pAC
    *  Where the first applies "a" passes singleton-arc-consistency
    *  and the second applies "b" passes arc consistency after every singleton check.
    * --------------------------------------------------
    *  onBeforePasses, doConsumePasses and onAfterPasses should
    *  handle the later case, while k-pSAC and variants are dealed in the loop() method.
    *  */

    protected void doConsumePasses() throws ContradictionException {
        if(willConsumePasses) {
            E.passesIncrement();
            while (E.hasPasses()) {
                for (int i = 0; i < E.passPropagatorsSize(); i++) {
                    Triple<Propagator<?>, Integer, Integer> args = E.passQueueAt(i);
                    Propagator<?>p = args.getFirst();
                    /* TODO: is this what we want ? */
                    if(!p.isPassive() && !p.isReified()) {
                        E.imperativeSchedule(p, args.getSecond(), args.getThird());
                    }
                }
                passConsumer();
                E.passesIncrement();
            }
            onAfterPasses();
        }
    }

    protected void onBeforePasses(){
        if(willConsumePasses){
            E.reinitPassBlacklist();
            E.__setDoConsumePasses(true);
            E.passesInit();
            E.initPassPropList();
        }
    }

    protected void onAfterPasses(){
        if(willConsumePasses){
            E.reinitPassBlacklist();
            E.__setDoConsumePasses(false);
            E.passesInit();
            E.initPassPropList();
        }
    }
}
