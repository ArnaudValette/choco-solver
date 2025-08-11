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

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.ISingletonConsistencyStrategy;
import org.chocosolver.solver.variables.Variable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;

/* Computes the time each method of an ISingletonConsistencyStrategy takes to run */
public class EfficiencyObserver implements ISingletonConsistencyStrategy {
    ISingletonConsistencyStrategy strategy;
    public HashMap<String, ArrayList<Long>> data;
    public HashMap<String, Long> starts;
    public long removes=0L;
    public long propagations=0L;
    public boolean isFirstProp=true;
    public long firstPropTimer=0L;


    public EfficiencyObserver(ISingletonConsistencyStrategy strategy) {
        this.strategy = strategy;
        this.data = new HashMap<>();
        this.starts = new HashMap<>();
    }

    protected void profile(String label, boolean isStarting){
        if(!data.containsKey(label)){
            data.put(label, new ArrayList<>());
        }
        if(isStarting){
            starts.put(label, System.nanoTime());
        }
        else{
            long start = starts.get(label);
            long duration = System.nanoTime() - start;
            data.get(label).add(duration);
        }
    }

    public ArrayList<Long> getTimeToPropagate(){
        return data.get("propagate");
    }

    @Override
    public void basePropagation() throws ContradictionException{
        strategy.basePropagation();

    }

    @Override
    public void propagate(SingletonConsistencyEngine engine) throws ContradictionException {
        if(isFirstProp){
            isFirstProp=false;
            firstPropTimer=System.nanoTime();
        }
        profile("propagate", true);
        strategy.propagate(engine);
        profile("propagate", false);
        propagations++;
    }

    @Override
    public void loop() throws ContradictionException {
        strategy.loop();
    }

    @Override
    public void task() throws ContradictionException {
        strategy.task();
    }

    @Override
    public boolean queueHandler(boolean changed) {
        boolean res = strategy.queueHandler(changed);
        return res;

    }

    @Override
    public ISingletonConsistencyStrategy ref() {
        return strategy.ref();
    }

    @Override
    public void setRef(ISingletonConsistencyStrategy ref) {
        strategy.setRef(ref);
    }

    @Override
    public void onBeforeAnything() {
        strategy.onBeforeAnything();
    }

    @Override
    public void onBeforeInstantiation() {
        strategy.onBeforeInstantiation();
    }

    @Override
    public void onAfterInstantiation() throws ContradictionException {
        strategy.onAfterInstantiation();
    }

    @Override
    public void onBeforeRemoval() {
        strategy.onBeforeRemoval();
    }

    @Override
    public void onAfterRemoval() throws ContradictionException {
        strategy.onAfterRemoval();
        removes++;
    }

    @Override
    public void onAfterSingletonFound() {
        strategy.onAfterSingletonFound();
    }

    @Override
    public void onAfterInstantiationPropagation() throws ContradictionException {
        strategy.onAfterInstantiationPropagation();
    }

    @Override
    public void buildBranch() {
        strategy.buildBranch();
    }

    @Override
    public void baseState() {
        strategy.baseState();
    }

    private void printTime(String label, long value){
        Duration d = Duration.ofNanos(value);
        long totalNanos = d.toNanos();
        long seconds = totalNanos / 1_000_000_000;
        long ms = (totalNanos / 1_000_000) % 1000;
        long us = (totalNanos / 1_000) % 1000;
        long ns = totalNanos % 1000;
        System.out.printf("\t%s: %ds:%03dms:%03dµs:%03dns\n",label, seconds, ms, us, ns);
    }

    private void printTime(String label, int value){
        System.out.printf("\t%s: %d\n",label, value);
    }

    public void printResults(){
        for(String s : data.keySet()){
            System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
            System.out.printf("\t%s \n\n",s);
            ArrayList<Long> d = data.get(s);
            long max=0;
            long sum=0;
            for(long datum : d){
                sum+=datum;
                if(max<datum){
                    max=datum;
                }
            }
            printTime("average", sum/Math.max(d.size(),1));
            printTime("max", max);
            printTime("full", sum);
            printTime("runs", d.size());
            System.out.println();
        }
        printTime("Nb of removeValue calls", removes);
    }

    @Override
    public boolean _isNeighborhoodAlgo() {
        return strategy._isNeighborhoodAlgo();
    }
}
