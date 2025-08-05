/*
 * This file is part of examples, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.examples;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.constraints.nary.alldifferent.AllDifferent;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.SAC1Strategy;
import org.chocosolver.solver.propagation.consistencyStrategy.SAC3Strategy;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDeg;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.function.Function;

public class KnightTour extends AbstractProblem{
    Model model;
    IntVar[] vars;
    Tuples legal;

    @Override
    public void buildModel() {
        model = new Model();
        vars= new IntVar[64];
        legal = new Tuples();
        for(int i = 0; i< 64; i++){
            for(int j =0; j<64; j++){
                if(isLegalMove(i,j)){
                    legal.add(i,j);
                }
            }
        }

        System.out.println(legal.toString());
        vars[0] = model.intVar(0);
        vars[1] = model.intVar(10);
        for(int i = 2; i<64; i++){
            vars[i] = model.intVar("C"+i, 0, 63);
        }

        for(int i = 0; i<63; i++){
            model.table(vars[i], vars[i+1], legal).post();
        }


        Constraint c1 = new AllDifferent(vars, "AC");
        model.post(c1);
        model.allDifferent(vars).post();
    }

    public boolean isLegalMove(int i, int j){
        int[] x = {2,1,2,1,-2,-1,-2,-1};
        int[] y = {1,2,-1,-2,1,2,-1,-2};
        for(int a = 0; a<x.length; a++){
            int xj = j/8, yj = j%8, xi = i/8, yi = i%8;
            if(xi+x[a] >= 0 && xi+x[a] < 8 && yi+y[a] >= 0 && yi+y[a] < 8){
                if(xi+x[a] == xj && yi+y[a] == yj){
                    return true;
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        new KnightTour().execute(args);
    }

    @Override
    public void configureSearch() {
        //model.getSolver().setEngine(new SingletonConsistencyEngine(model).enforceSAC3());
        //model.getSolver().setEngine(new SingletonConsistencyEngine(model).setPropagationStrategy(new SACNaiveDriverStrategy()));

        DomOverWDeg<IntVar> cacd = new DomOverWDeg<>(vars, 0);
        model.getSolver().setSearch(
                Search.intVarSearch(cacd, new IntDomainMin(), vars)
        );
        model.getSolver().showStatisticsDuringResolution(1000L);
    }

    @Override
    public void solve() {
        model.getSolver().setEngine(new SingletonConsistencyEngine(model).setPropagationStrategy(new SAC1Strategy()));
        model.getSolver().solve();
        model.getSolver().printStatistics();
    }
}