/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.memory.IEnvironment;
import org.chocosolver.sat.MiniSat;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.PropagationEngine;
import org.chocosolver.solver.variables.IntVar;

public class TestSAC extends PropagationEngine {
    Solver solver;
    IEnvironment env;

    public TestSAC(Model model, MiniSat sat) {
        super(model, sat);
        solver = model.getSolver();
        env = solver.getEnvironment();
    }

    @Override
    public void propagate() throws ContradictionException {
        super.propagate();
        loop();
    }

    public void loop() throws ContradictionException {
        boolean changed = false;
        do {
            changed = false;
            IntVar[] vars = model.retrieveIntVars(true);
            for (IntVar v : vars) {
                for (int value = v.getLB(); value <= v.getUB(); value = v.nextValue(value)) {
                    int id = env.getWorldIndex();
                    try {
                        env.worldPush();
                        v.instantiateTo(value, Cause.Null);
                        super.propagate();
                        env.worldPopUntil(id);
                        flush();
                    }
                    catch (ContradictionException e){
                        env.worldPopUntil(id);
                        flush();
                        v.removeValue(value, Cause.Null);
                        super.propagate();
                        flush();
                        changed = true;
                    }
                }
            }
        } while(changed);
    }
}
