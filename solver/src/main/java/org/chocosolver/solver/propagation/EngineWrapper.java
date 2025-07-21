package org.chocosolver.solver.propagation;

import org.chocosolver.memory.IEnvironment;
import org.chocosolver.sat.MiniSat;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.PropagatorEventType;

public class EngineWrapper extends AbstractEngine implements IPropagationEngine{
    Model model;
    Solver solver;
    IEnvironment env;
    PropagationEngine pe;

    public EngineWrapper(Model model, MiniSat sat){
        super();
        pe = new PropagationEngine(model, sat);
        parent = this;
        this.model = model;
        env = model.getEnvironment();
        solver = model.getSolver();
        pe.setParent(this);
    }

    @Override
    public void initialize() throws SolverException {
        pe.initialize();
    }

    @Override
    public void propagate() throws ContradictionException{
        pe.propagate();
    }

    @Override
    public boolean isInitialized() {
        return pe.isInitialized();
    }


    @Override
    public void execute(Propagator<?> propagator)throws ContradictionException{
        pe.execute(propagator);
    }

    @Override
    public void flush(){
        pe.flush();
    }

    @Override
    public void onVariableUpdate(Variable variable, IEventType type, ICause cause){}

    @Override
    public void schedule(Propagator<?> prop, int pindice, int mask){
        pe.schedule(prop, pindice, mask);
    }

    @Override
    public void delayedPropagation(Propagator<?> propagator, PropagatorEventType type){
        pe.delayedPropagation(propagator, type);
    }

    @Override
    public int getDelayedPropagation(){
        return pe.getDelayedPropagation();
    }

    @Override
    public void onPropagatorExecution(Propagator<?> propagator){
        pe.onPropagatorExecution(propagator);
    }

    @Override
    public void deactivatePropagator(Propagator<?> propagator){
        pe.deactivatePropagator(propagator);
    }

    @Override
    public void setInsight(PropagationInsight insight){
        pe.setInsight(insight);
    }

    @Override
    public void setHybrid(byte hybrid){
        pe.setHybrid(hybrid);
    }

    @Override
    public void reset(){
        pe.reset();
    }

    @Override
    public void clear(){
        pe.clear();
    }

    @Override
    public void ignoreModifications(){
        pe.ignoreModifications();
    }

    @Override
    public void dynamicAddition(boolean permanent, Propagator<?>... ps) throws SolverException {
        pe.dynamicAddition(permanent, ps);
    }

    @Override
    public void updateInvolvedVariables(Propagator<?> p){
        pe.updateInvolvedVariables(p);
    }

    @Override
    public void propagateOnBacktrack(Propagator<?> propagator){
        pe.propagateOnBacktrack(propagator);
    }

    @Override
    public void dynamicDeletion(Propagator<?>...ps){
        pe.dynamicDeletion(ps);
    }

}
