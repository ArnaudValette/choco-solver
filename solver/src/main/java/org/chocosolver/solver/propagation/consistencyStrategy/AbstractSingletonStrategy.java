package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.variables.IntVar;
import org.jgrapht.alg.util.Triple;

public abstract class AbstractSingletonStrategy<T> extends BaseStrategy<T> {
    protected SingletonConsistencyEngine E;
    protected IntVar Xi;
    protected Integer Aj;
    protected boolean willConsumePasses = false;
    protected int lastId;

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                                         Abstract Methods
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /** @void The method used to consume passes in k-pXXX variants of our consistencies (e.g. simply Engine.doPropagate()
     * in the case of simpler algorithms such as 1-pSAC1). */
    protected abstract void passConsumer() throws ContradictionException;

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                                         Core Method
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public void propagate(SingletonConsistencyEngine engine) throws ContradictionException {
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
                    onBeforeInstantiation();
                    Xi.instantiateTo(Aj, Cause.Null);
                    onAfterInstantiation(); /* e.g. E.doPropagate() */
                    E.worldPopUntilNFlush(lastId);

                } catch (ContradictionException ce) {
                    E.worldPopUntilNFlush(lastId);
                    onBeforeRemoval();
                    Xi.removeValue(Aj, Cause.Null);
                    onAfterRemoval(); /* e.g. E.doPropagate() */
                    changed = true;
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
        baseState();
    };

    protected void onBeforeRemoval(){ baseState(); };

    protected void onAfterRemoval() throws ContradictionException {
        /* base case : just propagate */
        E.doPropagate();
        baseState();
    };

    protected void baseState(){
        E.setDoFilterScheduling(false);
        E.setCheckSingleton(false);
        E.setDoConsumePasses(false);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *                                         Passes Management
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public AbstractSingletonStrategy setWillConsumePasses(boolean b){
        willConsumePasses =b;
        return this;
    }

    protected void doConsumePasses() throws ContradictionException {
        if(willConsumePasses) {
            E.passesIncrement();
            while (E.hasPasses()) {
                for (int i = 0; i < E.passPropagatorsSize(); i++) {
                    Triple<Propagator<?>, Integer, Integer> args = E.passQueueAt(i);
                    E.imperativeSchedule(args.getFirst(), args.getSecond(), args.getThird());
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
            E.setDoConsumePasses(true);
            E.passesInit();
            E.initPassPropList();
        }
    }

    protected void onAfterPasses(){
        if(willConsumePasses){
            E.reinitPassBlacklist();
            E.setDoConsumePasses(false);
            E.passesInit();
            E.initPassPropList();
        }
    }
}
