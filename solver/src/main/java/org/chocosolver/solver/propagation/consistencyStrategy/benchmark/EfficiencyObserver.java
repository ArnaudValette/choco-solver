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
    public ArrayList<Long> pruning;
    public ArrayList<Long> size;
    public long removes=0L;
    public long propagations=0L;
    public boolean isFirstProp=true;
    public long firstPropTimer=0L;


    public EfficiencyObserver(ISingletonConsistencyStrategy strategy) {
        this.strategy = strategy;
        this.data = new HashMap<>();
        this.starts = new HashMap<>();
        this.pruning = new ArrayList<>();
        this.size = new ArrayList<>();
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
        profile("basePropagation", true);
        strategy.basePropagation();
        profile("basePropagation", false);

    }

    @Override
    public void propagate(SingletonConsistencyEngine engine) throws ContradictionException {
        if(isFirstProp){
            isFirstProp=false;
            firstPropTimer=System.nanoTime();
            System.out.println(firstPropTimer);
        }
        long sumStart = 0L;
        for(Variable v : engine.getVars()){
            sumStart+=v.getDomainSize();
        }
        profile("propagate", true);
        strategy.propagate(engine);
        profile("propagate", false);
        long sumEnd=0L;
        long tempSize = 0L;
        for(Variable v : engine.getVars()){
            sumEnd+=v.getDomainSize();
            tempSize+=v.getDomainSize();
        }
        pruning.add(sumStart-sumEnd);
        size.add(tempSize);
        propagations++;
    }

    @Override
    public void loop() throws ContradictionException {
        profile("loop", true);
        strategy.loop();
        profile("loop", false);
    }

    @Override
    public void task() throws ContradictionException {
        profile("task",true);
        strategy.task();
        profile("task", false);
    }

    @Override
    public boolean queueHandler(boolean changed) {
        profile("task",true);
        boolean res = strategy.queueHandler(changed);
        profile("queueHandler",false);
        return res;

    }

    @Override
    public ISingletonConsistencyStrategy ref() {
        return strategy.ref();
    }

    @Override
    public void setRef(ISingletonConsistencyStrategy ref) {
        profile("setRef",true);
        strategy.setRef(ref);
        profile("setRef",false);
    }

    @Override
    public void onBeforeAnything() {
        profile("onBeforeAnything",true);
        strategy.onBeforeAnything();
        profile("onBeforeAnything",false);
    }

    @Override
    public void onBeforeInstantiation() {
        profile("onBeforeInstantiation",true);
        strategy.onBeforeInstantiation();
        profile("onBeforeInstantiation",false);
    }

    @Override
    public void onAfterInstantiation() throws ContradictionException {
        profile("onAfterInstantiation",true);
        strategy.onAfterInstantiation();
        profile("onAfterInstantiation",false);
    }

    @Override
    public void onBeforeRemoval() {
        profile("onBeforeRemoval",true);
        strategy.onBeforeRemoval();
        profile("onBeforeRemoval",false);
    }

    @Override
    public void onAfterRemoval() throws ContradictionException {
        removes++;
        profile("onAfterRemoval",true);
        strategy.onAfterRemoval();
        profile("onAfterRemoval",false);
    }

    @Override
    public void onAfterSingletonFound() {
        profile("onAfterSingletonFound",true);
        strategy.onAfterSingletonFound();
        profile("onAfterSingletonFound",false);
    }

    @Override
    public void onAfterInstantiationPropagation() throws ContradictionException {
        profile("onAfterInstantiationPropagation",true);
        strategy.onAfterInstantiationPropagation();
        profile("onAfterInstantiationPropagation",false);
    }

    @Override
    public void buildBranch() {
        profile("buildBranch",true);
        strategy.buildBranch();
        profile("buildBranch",false);
    }

    @Override
    public void baseState() {
        profile("baseState",true);
        strategy.baseState();
        profile("baseState",false);
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
        long sum = 0;
        for(long datum : pruning){
            sum+=datum;
        }
        long time = 0;
        for(long x : data.get("propagate")){
            time+= x;
        }
        printTime("Pruning (average per propagation)", sum/Math.max(pruning.size(), 1));
        printTime("Nb of removeValue calls", removes);
        printTime("Average time to prune values", time/Math.max(sum/Math.max(pruning.size(),1),1));
    }
}
