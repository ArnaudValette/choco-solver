package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.variables.Variable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;

public class EfficiencyObserver implements ISingletonConsistencyStrategy{
    ISingletonConsistencyStrategy strategy;
    HashMap<String, ArrayList<Long>> data;
    HashMap<String, Long> starts;
    ArrayList<Integer> pruning;
    int removes;


    public EfficiencyObserver(ISingletonConsistencyStrategy strategy) {
        this.strategy = strategy;
        this.data = new HashMap<>();
        this.starts = new HashMap<>();
        this.pruning = new ArrayList<>();
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
    @Override
    public void propagate(SingletonConsistencyEngine engine) throws ContradictionException {
        int sumStart = 0;
        for(Variable v : engine.getVars()){
            sumStart+=v.getDomainSize();
        }
        profile("propagate", true);
        strategy.propagate(engine);
        profile("propagate", false);
        int sumEnd=0;
        for(Variable v : engine.getVars()){
            sumEnd+=v.getDomainSize();
        }
        pruning.add(sumStart-sumEnd);
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
            printTime("average", sum/d.size());
            printTime("max", max);
            printTime("full", sum);
            printTime("runs", d.size());
            System.out.println();
        }
        int sum = 0;
        for(int datum : pruning){
            sum+=datum;
        }
        long time = 0;
        for(long x : data.get("propagate")){
            time+= x;
        }
        printTime("Pruning (average per propagation)", sum/pruning.size());
        printTime("Nb of removeValue calls", removes);
        printTime("Average time to prune values", time/(sum/pruning.size()) );
    }
}
