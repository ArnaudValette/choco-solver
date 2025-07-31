package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.memory.IEnvironment;
import org.chocosolver.sat.MiniSat;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.jgrapht.alg.util.Pair;

import java.util.LinkedList;

public class SAC3 extends SAC1 {

    Solver solver;
    IEnvironment env;
    LinkedList<Pair<IntVar,Integer>> Q;
    int lastId;

    public SAC3(Model model, MiniSat sat) {
        super(model, sat);
        solver = model.getSolver();
        env = solver.getEnvironment();
        hybrid = 0b10;
    }

    @Override
    public void propagate() throws ContradictionException {
        doPropagate();
        loop();
    }

    protected void reinitQ(){
        LinkedList<Pair<IntVar, Integer>> queue = new LinkedList<>();
        IntVar[] vars = model.retrieveIntVars(true);
        for(IntVar v : vars) {
            for (int value = v.getLB(); value <= v.getUB(); value = v.nextValue(value)) {
                queue.add(new Pair<>(v, value));
            }
        }
        Q=queue;
    }

    @Override
    public void loop() throws ContradictionException {
        boolean changed = false;
        reinitQ();
        while (!Q.isEmpty()) {
            Pair<IntVar, Integer> p = Q.pop();
            IntVar v = p.getFirst();
            int value = p.getSecond();
            if(v.contains(value)) {
                lastId = env.getWorldIndex();
                env.worldPush();
                try {
                    instantiate(v, value);
                    env.worldPush();
                    doPropagate();
                    flush();
                    buildBranch();
                } catch (ContradictionException e) {
                    flush();
                    env.worldPopUntil(lastId);
                    removeValue(v, value);
                    doPropagate();
                    changed = true;
                }
            }
            if (Q.isEmpty() && changed) {
                reinitQ();
                changed = false;
            }
        }
    }

    private void buildBranch(){
        int i = 0;
        int t = Q.size();
        while(i<t){
            Pair<IntVar, Integer> p = Q.pop();
            IntVar v = p.getFirst(); int value = p.getSecond();
            i++;
            if(!v.contains(value)){
                Q.add(p);
                continue;
            }
            try{
                env.worldPush();
                instantiate(v, value);
                doPropagate();
            } catch (ContradictionException e) {
                flush();
                Q.add(p);
                break;
            }
        }
        env.worldPopUntil(lastId);
    }
}
