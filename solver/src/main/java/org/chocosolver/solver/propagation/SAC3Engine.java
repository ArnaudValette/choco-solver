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
import org.chocosolver.solver.constraints.graph.symmbreaking.Pair;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;


public class SAC3Engine extends PropagationEngine{
    Pair<IntVar,Integer>[] allPair;
    ArrayDeque<Integer> pendingQueue;
    public SAC3Engine(Model model, MiniSat sat) {
        super(model, sat);
    }


    @Override
    public void initialize() throws SolverException {
        super.initialize();
        pendingQueue = new ArrayDeque<>();

        int size = 0;
        for (IntVar var : model.retrieveIntVars(true)){
            size += var.getDomainSize();
        }
        allPair = new Pair[size];

        List<IntVar> vars = new ArrayList<>(Arrays.asList(model.retrieveIntVars(true)));
        Collections.shuffle(vars);

        int k = 0;
        for (IntVar var : vars){
            Iterator<Integer> it;
            for (it = var.iterator(); it.hasNext();){
                allPair[k] = new Pair<>(var, it.next());
                k ++;
            }
        }
        initPendingQueue();
    }

    private void initPendingQueue(){
        for (int i = 0; i < allPair.length; i++) {
            if(inModel(allPair[i])){
                pendingQueue.add(i);
            }
        }
    }

    @Override
    public void propagate() throws ContradictionException {
        basePropagation();
        singletonArcConsistency();
    }

    public void singletonArcConsistency() throws ContradictionException {
        Pair<IntVar, Integer> pair;
        IntVar var;
        Integer val;
        initPendingQueue();
        boolean changed = false;

        // TODO better stop criterion
        while (!pendingQueue.isEmpty() && !model.getSolver().isStopCriterionMet()) {
            int index = pendingQueue.pop();
            pair = allPair[index];
            var = pair.getA();
            val = pair.getB();
            if (!var.contains(val))
                continue;

            if (!buildBranch(var, val)){
                var.removeValue(val, Cause.Null);
                basePropagation();
                changed = true;
            }

            if (pendingQueue.isEmpty() && changed){
                initPendingQueue();
                changed = false;
            }
        }

    }

    private boolean buildBranch(IntVar x, Integer a){
        model.getEnvironment().worldPush();

        try{
            x.instantiateTo(a, Cause.Null);
            basePropagation();

        } catch (ContradictionException e) {
            flush();
            model.getEnvironment().worldPop();
            return false;
        }

        Pair<IntVar, Integer> pair;
        IntVar var;
        Integer val;
        int index;
        pendingQueue.addLast(-1);
        // TODO better stop criterion
        while(!pendingQueue.isEmpty() && !model.getSolver().isStopCriterionMet()){
            index = pendingQueue.pop();
            if (index == -1){
                flush();
                model.getEnvironment().worldPop();
                return true;
            }

            pair = allPair[index];
            if(!inModel(pair)){
                pendingQueue.addLast(index);
                continue;
            }

            var = pair.getA();
            val = pair.getB();

            try{
                var.instantiateTo(val, Cause.Null);
                basePropagation();
            } catch (ContradictionException e) {
                flush();
                pendingQueue.addFirst(index);
                pendingQueue.remove(-1);
                model.getEnvironment().worldPop();
                return true;
            }
        }
        flush();
        pendingQueue.remove(-1);
        model.getEnvironment().worldPop();
        return true;
    }

    private boolean inModel(Pair<IntVar,Integer> pair){
        return pair.getA().contains(pair.getB());
    }
}
