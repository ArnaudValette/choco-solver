package org.chocosolver.solver.propagation.consistencyStrategy.types;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.AbstractSingletonStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;

import java.util.BitSet;
import java.util.Set;

public abstract class VariableBasedStrategy extends AbstractSingletonStrategy<IntVar> {
    protected BitSet nsac;
    protected BitSet rnsac;
    protected Set<IntVar> nx;


    public void setQ(ReinitialisableQueue<IntVar> q) { Q=q; }

    public void loop()throws ContradictionException {
        while(!Q.isEmpty()){
            changed=false;
            IntVar v = Q.pop();
            rnsac=E.getDirectOnlyBlacklist(v);
            nsac=E.getNsacBlacklist(v);
            nx = E.getNeighborhood(v);
            for(int value = v.getLB(); value <= v.getUB(); value=v.nextValue(value)){
                Xi = v; Aj = value;
                task();
            }
        }
    }
}
