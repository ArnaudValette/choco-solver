package org.chocosolver.solver.propagation.consistencyStrategy.types;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.ISingletonConsistencyStrategy;
import org.jgrapht.alg.util.Triple;

public interface PassConsumer extends ISingletonConsistencyStrategy {
    default void doConsumePasses(SingletonConsistencyEngine E) throws ContradictionException {
        if(willConsumePasses()){
            E.passesIncrement();
            while(E.hasPasses()){
                for(int i =0; i<E.passPropagatorsSize(); i++){
                    Triple<Propagator<?>, Integer, Integer> args = E.passQueueAt(i);
                    E.imperativeSchedule(args.getFirst(), args.getSecond(), args.getThird());
                }
                centralRoutine(E);
                E.passesIncrement();
            }
            onAfterPasses(E);
        }
    }
    void centralRoutine(SingletonConsistencyEngine E) throws ContradictionException;
    PassConsumer consumePasses();
    PassConsumer ignorePasses();
    boolean willConsumePasses();

    default void onBeforePasses(SingletonConsistencyEngine E){
        E.reinitPassBlacklist();
        E.setDoConsumePasses(willConsumePasses());
        E.passesInit();
        E.initPassPropList();
    }

    default void onAfterPasses(SingletonConsistencyEngine E){
        E.reinitPassBlacklist();
        E.setDoConsumePasses(false);
        E.passesInit();
        E.initPassPropList();
    }
}
