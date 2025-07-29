package org.chocosolver.solver.propagation.consistencyStrategy.types;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.variables.IntVar;

public abstract class AbstractSAC1 extends VariableBasedStrategy{
    @Override
    public void propagate(SingletonConsistencyEngine engine) throws ContradictionException {
        if(Q == null){
            E.provideQ(this);
        }
        changed = false;
        E=engine;
        onBeforeAnything();
        E.doPropagate();
        // Q.reinit(); reinitialised in loop()
        loop();
    }
    @Override
    public void loop() throws ContradictionException {
        /* This is the reason this class exist,
         * Using SAC1 with the VariableBasedStrategy.loop() logic
         * leads to lesser efficiency (greater amount of nodes)
         * */
        do{
            changed = false;
            Q.reinit();
            while(!Q.isEmpty()){
                IntVar v = Q.pop();
                rnsac=E.getDirectOnlyBlacklist(v);
                nsac=E.getNsacBlacklist(v);
                nx = E.getNeighborhood(v);
                for(int value = v.getLB(); value <= v.getUB(); value=v.nextValue(value)){
                    Xi = v; Aj = value;
                    task();
                }
            }
        } while (changed);
    }

    @Override
    protected boolean queueHandler(boolean changed){
        /* Don't do anything fancy */
        return changed;
    }
}
