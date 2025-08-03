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
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.IntVar;

public class SACEngine extends PropagationEngine{

    IntVar[] vars;
    int[] weights ;

    public SACEngine(Model model, MiniSat sat) {
        super(model, sat);
    }

    @Override
    public void initialize() throws SolverException {
        super.initialize();
        vars = model.retrieveIntVars(true);
        weights = new int[vars.length];
    }

    @Override
    public void propagate() throws ContradictionException {
        basePropagation();
        singletonArcConsistency();
    }

    protected void singletonArcConsistency() throws ContradictionException {
        IntVar var;
        boolean propFailed;
        int i = 0;
        // TODO better stop criterion
        while (i < vars.length && !model.getSolver().isStopCriterionMet()) {
            var = vars[i];

            for (int val = var.getLB();
                 val <= var.getUB() && !model.getSolver().isStopCriterionMet();
                 val = var.nextValue(val)) {
                model.getEnvironment().worldPush();

                propFailed = false;
                try{
                    hasSupport(var, val);
                } catch (ContradictionException e){
                    propFailed = true;
                }

                flush();
                model.getEnvironment().worldPop();

                if (propFailed){

                    var.removeValue(val, Cause.Null);
                    basePropagation();

                    if (i>=0){
                        updateWeights(i);
                    }

                    i = -1;
                }
            }
            i++;
        }
    }

    protected void hasSupport(IntVar var, int val) throws ContradictionException {
        var.instantiateTo(val, Cause.Null);
        basePropagation();
    }

    private void updateWeights(int i){
        weights[i]++;
        int j = i;
        while (j>0 && weights[j-1]<weights[i]){
            j--;
        }
        int tempWeight = weights[j];
        weights[j] = weights[i];
        weights[i] = tempWeight;

        IntVar tempVar = vars[j];
        vars[j] = vars[i];
        vars[i] = tempVar;
    }
}
