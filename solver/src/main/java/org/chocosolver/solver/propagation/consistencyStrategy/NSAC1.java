package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.sat.MiniSat;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;

import java.util.*;
import java.util.stream.Collectors;

public class NSAC1 extends SAC1{
    HashMap<IntVar, BitSet> NSAC_PROPS_MAP = new HashMap<>();
    BitSet currentNsacBlackList = new BitSet();
    HashMap<IntVar, Set<IntVar>> neighborhood = new HashMap<>();

    public NSAC1(Model model, MiniSat sat) {
        super(model, sat);
    }

    @Override
    public void initialize() throws SolverException {
        super.initialize();
        /* Initialize blacklist hashmap */
        for (IntVar v : model.retrieveIntVars(true)) {
            NSAC_PROPS_MAP.put(v, new BitSet());
            for (Propagator<?> prop : propagators) {
                NSAC_PROPS_MAP.get(v).set(prop.hashCode());
            }
        }

        /* for Xi, whitelist direct propagators */
        for(Propagator<?> p : propagators){
            for(Variable v : p.getVars()){
                NSAC_PROPS_MAP.get(v).clear(p.hashCode());
                if(!neighborhood.containsKey(v)){
                    neighborhood.put((IntVar) v, new HashSet<>());
                }
                for(Variable u : p.getVars()){
                    if(!u.equals(v)){
                        neighborhood.get((IntVar) v).add((IntVar) u);
                    }
                }
            }
        }

        /* For Xi, whitelist propagators that concerns at least 2 neighbors */
        for(IntVar v : model.retrieveIntVars(true)){
            Set<IntVar>  set = neighborhood.get(v);
            if(set != null){
                for(Propagator<?> p : propagators){
                    Set<Variable> pSet = Arrays.stream(p.getVars()).collect(Collectors.toSet());
                    int matches = 0;
                    for(Variable pVar : pSet){
                        if(set.contains((IntVar) pVar)){
                            matches++;
                        }
                        if(matches >= 2){
                            NSAC_PROPS_MAP.get(v).clear(p.hashCode());
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void doPropagate() throws ContradictionException {
        currentNsacBlackList = new BitSet();
        super.doPropagate();
    }

    @Override
    public void instantiate(IntVar X, int a) throws ContradictionException {
        currentNsacBlackList = NSAC_PROPS_MAP.get(X);
        super.instantiate(X, a);
    }

    @Override
    public void removeValue(IntVar X, int a) throws ContradictionException {
        currentNsacBlackList = new BitSet();
        super.removeValue(X, a);
    }

    @Override
    public void schedule(Propagator<?> prop, int pindice, int mask) {
        if(!currentNsacBlackList.get(prop.hashCode())) {
            super.schedule(prop, pindice, mask);
        }
    }
}
