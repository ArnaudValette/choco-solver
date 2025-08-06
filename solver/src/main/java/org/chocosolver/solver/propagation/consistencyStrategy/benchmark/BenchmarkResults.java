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

import java.time.Duration;

/* Should provide:
* time to solve
* nodes
* fails
* number of propagations (PropagationEngine)
* number of propagations (SingletonConsistencyEngine)
* average number of pruned values per propagation
* average number of pruned values per second
* time to propagate (Long[])
*
* would be nice to find a way to represent the size of the problem at each propagation,
* we could then show the evolution of a problem during resolution
* */
public class BenchmarkResults {
    SingletonConsistencyEngine E;
    EfficiencyObserver O;
    long timeToSolve;
    long nodes;
    long fails;
    long _propagations;
    long propagations;
    double averagePerProp;
    double averagePerSeconds;


    public BenchmarkResults(SingletonConsistencyEngine consistencyEngine, EfficiencyObserver obs) {
        E=consistencyEngine;
        O=obs;
    }

    public void main(){
        timeToSolve = System.nanoTime() - O.firstPropTimer;
        nodes = E.getModel().getSolver().getNodeCount();
        fails = E.getModel().getSolver().getFailCount();
        _propagations = E.getModel().getSolver().getPropagationCount();
        propagations = O.propagations;
        Long tmp = O.pruning.stream().reduce(0L,(res,el)->res+el);
        averagePerProp = (double)tmp/O.propagations;
        double timeToSolveSec = (double)timeToSolve/1_000_000_000.0;
        averagePerSeconds = (double)tmp/timeToSolveSec;

        printTime("Time to solve", timeToSolve);
        System.out.println("Number of nodes: " + nodes);
        System.out.println("Number of failures: " + fails);
        System.out.println("Number of propagations (innerEngine): " + _propagations);
        System.out.println("Number of propagations (outerEngine): " + propagations);
        System.out.println("Average pruning per propagation: " + averagePerProp);
        System.out.println("Average pruning per second: " + averagePerProp + "/s");
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

}
