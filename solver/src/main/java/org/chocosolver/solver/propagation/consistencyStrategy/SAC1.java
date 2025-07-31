package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.memory.IEnvironment;
import org.chocosolver.sat.MiniSat;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.PropagationEngine;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.iterators.DisposableValueIterator;

public class SAC1 extends PropagationEngine implements ICause {
    Solver solver;
    IEnvironment env;
    int nbPropagations=0;

    public SAC1(Model model, MiniSat sat) {
        super(model, sat);
        solver = model.getSolver();
        env = solver.getEnvironment();
        hybrid = 0b10;
    }

    @Override
    public void propagate() throws ContradictionException {
        nbPropagations++;
        doPropagate();
        getDelayedPropagation();
        loop();
    }

    public void doPropagate() throws ContradictionException {
        super.propagate();
    }


    public void instantiate(IntVar X, int a) throws ContradictionException {
        X.instantiateTo(a, this);
    }

    public void removeValue(IntVar X, int a) throws ContradictionException{
        X.removeValue(a, this);
    }

    public void loop() throws ContradictionException {
        boolean changed = false;
        IntVar[] vars = model.retrieveIntVars(true);
        do {
            changed = false;
            for (int i = 0 ; i< vars.length; i++) {
                IntVar v = vars[i];
                if(!v.isInstantiated()) {
                    DisposableValueIterator it = v.getValueIterator(true);
                    while(it.hasNext()) {
                        int value = it.next();
                        int id = env.getWorldIndex();
                        try {
                            env.worldPush();
                            instantiate(v, value);
                            super.propagate();
                            env.worldPopUntil(id);
                            flush();
                        } catch (ContradictionException e) {
                            env.worldPopUntil(id);
                            flush();
                            removeValue(v, value);
                            super.propagate();
                            changed = true;
                        }
                    }
                    it.dispose();
                }
            }
        } while(changed);
    }
}
