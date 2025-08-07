/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.propagation.consistencyStrategy.benchmark;

import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.ISingletonConsistencyStrategy;
import org.chocosolver.solver.propagation.consistencyStrategy.SAC3Strategy;

import java.time.Duration;
import java.util.Date;
import java.util.List;

/* Should provide:
* time to solve
* nodes
* fails
* number of propagations (PropagationEngine)
* number of propagations (SingletonConsistencyEngine)
* average number of pruned values per propagation
* average number of pruned values per second
*
* time to propagate (Long[])
*
* would be nice to find a way to represent the size of the problem at each propagation,
* we could then show the evolution of a problem during resolution
* */
public class BenchmarkResults {
    boolean committed = false;
    SingletonConsistencyEngine E;
    EfficiencyObserver O;
    String instance;
    String consistency;
    long timeToSolve;
    long nodes;
    long variables;
    long constraints;
    long backtracks;
    long fails;
    long _propagations;
    long propagations;
    double averagePropPerSeconds;
    double averagePerProp;
    double averagePerSeconds;
    List<Long> propData;
    List<Long> sizeData;
    List<Long> pruneData;
    boolean solved;


    public BenchmarkResults(SingletonConsistencyEngine consistencyEngine, EfficiencyObserver obs) {
        E=consistencyEngine;
        O=obs;
    }

    public void commit(){
        timeToSolve = System.nanoTime() - O.firstPropTimer;
        nodes = E.getModel().getSolver().getNodeCount();
        variables = E.getModel().getNbVars();
        constraints = E.getModel().getNbCstrs();
        fails = E.getModel().getSolver().getFailCount();
        backtracks = E.getModel().getSolver().getBackTrackCount();
        _propagations = E.getModel().getSolver().getPropagationCount();
        propagations = O.propagations;
        Long tmp = O.pruning.stream().reduce(0L,(res,el)->res+el);
        averagePerProp = (double)tmp/O.propagations;
        double timeToSolveSec = (double)timeToSolve/1_000_000_000.0;
        averagePerSeconds = (double)tmp/timeToSolveSec;
        averagePropPerSeconds = (double)propagations/timeToSolveSec;
        propData = O.getTimeToPropagate();
        sizeData = O.size;
        pruneData = O.pruning;
        instance = E.getModel().getName();
        consistency = O.strategy.getClass().toString();
        solved = E.getModel().getSolver().getSolutionCount() > 0;
        committed=true;
    }

    private void printTime(String label, long value){
        Duration d = Duration.ofNanos(value);
        long totalNanos = d.toNanos();
        long seconds = totalNanos / 1_000_000_000;
        long ms = (totalNanos / 1_000_000) % 1000;
        long us = (totalNanos / 1_000) % 1000;
        long ns = totalNanos % 1000;
        System.out.printf("%s: %ds:%03dms:%03dµs:%03dns\n",label, seconds, ms, us, ns);
    }

    public String toJSON(){
        return "{ " +
                toJSON("instance", instance) + ", " +
                toJSON("consistency", consistency) + ", " +
                toJSON("date", new Date()) + ", " +
                toJSON("solved", solved) + ", " +
                toJSON("constraints", constraints) + ", " +
                toJSON("variables", variables) + ", " +
                toJSON("timeToSolve", timeToSolve) + ", " + toJSON("nodes", nodes) + ", " +
                toJSON("fails", fails) + ", " +
                toJSON("backtracks", backtracks) + ", " +
                toJSON("innerPropagations", _propagations) + ", " +
                toJSON("propagations", propagations) + ", " +
                toJSON("averagePropPerSeconds", averagePropPerSeconds) + ", " +
                toJSON("averagePruningPerProp", averagePerProp) + ", " +
                toJSON("averagePruningPerSeconds", averagePerSeconds) + ", " +
                toJSON("propData", propData) + ", " +
                toJSON("sizeData", sizeData) + ", " +
                toJSON("pruneData", pruneData) +
                " \n}";
    }

    private String toJSON(String label, boolean bool){
        return "\n\"" + label + "\" : " +  bool ;
    }
    private String toJSON(String label, Date date){
        return "\n\"" + label + "\" : \"" + date + "\"";
    }
    private String toJSON(String label, long l){
        return "\n\"" + label + "\" : " + l;
    }
    private String toJSON(String label, String s){
        return "\n\"" + label + "\" : \"" + s + "\"";
    }

    private String toJSON(String label, double d){
        return "\n\"" + label + "\" : " + d;
    }

    private String toJSON(String label, List<Long> l){
        if(l == null){
            System.out.println(label + " is null");
            return "\n\"" + label + "\" : null";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < l.size(); i++) {
                sb.append(l.get(i));
                if (i != l.size() - 1) {
                    sb.append(", ");
                }
            }
            String s = sb.toString();
            return "\n\"" + label + "\" : [ " + s + " ]";
        }
    }

}
