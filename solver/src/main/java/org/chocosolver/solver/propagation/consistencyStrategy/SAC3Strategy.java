package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.consistencyStrategy.types.PairBasedStrategy;
import org.chocosolver.solver.variables.IntVar;

public class SAC3Strategy extends PairBasedStrategy {

    @Override
    protected void onAfterInstantiation() throws ContradictionException {
            E.doPropagate();
            buildBranch();
            baseState();
    }

    private void buildBranch(){
        int i = 0;
        int t = Q.size();
        while(i<t) {
            Pair<IntVar, Integer> p = Q.pop();
            IntVar Xn = p.getA();
            Integer Am = p.getB();
            i++;
            if(!Xn.contains(Am)) {
                Q.add(p);
                continue;
            }
            try {
                E.worldPush();
                instantiate(Xn, Am);
                E.doPropagate();
            } catch (ContradictionException ignore) {
                Q.add(p);
                break;
            }
        }
        E.worldPopUntilNFlush(lastId);
    }

    @Override
    protected void passConsumer() throws ContradictionException {
        /* TODO k-pSAC3 */
    }
}
