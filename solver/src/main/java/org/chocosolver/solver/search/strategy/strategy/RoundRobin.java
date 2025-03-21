/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.strategy.strategy;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.loop.monitors.IMonitorRestart;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainBest;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainLast;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.bandit.Policy;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;

/**
 * A search strategy that selects the next strategy to apply in a round-robin fashion.
 * A strategy is composed of a variable selector and a value selector.
 * On each restart, the strategy is updated to the next one in the list of strategies.
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 11/10/2024
 */
public class RoundRobin extends AbstractStrategy<IntVar> implements IMonitorRestart { //}, IMonitorSolution {

    /**
     * A strategy that does nothing but return null.
     * It is used to set the main strategy to null in meta-strategies
     */
    private static class NullStrategy extends AbstractStrategy<IntVar> {

        protected NullStrategy() {
            super(new IntVar[0]);
        }

        @Override
        public Decision<IntVar> getDecision() {
            return null;
        }
    }

    // Singleton of the null strategy
    public static final AbstractStrategy<IntVar> NULL_STRATEGY = new NullStrategy();

    // Order of importance of the selectors, the smaller the index, the quicker the selector is changed
    private static final int VALUE = 0;
    private static final int VARIABLE = 1;
    private static final int META = 2;


    // the meta strategies to select the next strategy to apply
    private final MetaStrategy<IntVar>[] metaStrategies;
    // the variable selectors to select the next variable to branch on
    private final VariableSelector<IntVar>[] variableSelectors;
    // the value selectors to select the next value to assign to the selected variable
    private final IntValueSelector[] valueSelectors;

    // true if the solution has to be saved
    Function<IntVar, OptionalInt> solutionFirst = v -> OptionalInt.empty();
    Function<IntVar, OptionalInt> bestFirst = v -> OptionalInt.empty();

    private final Policy bandit;
    private final ToDoubleBiFunction<Integer, Integer> reward;

    private final List<int[]> combinations;
    private int action;
    private int step;

    /**
     * Creates a RoundRobin strategy.
     * The strategy selects the next variable to branch on using the variable selectors in the order they are given.
     * The strategy selects the next value to assign to the selected variable using the value selectors in the order they are given.
     * The selectors are changed on each restart.
     *
     * @param variables              the variables to branch on
     * @param variableSelectors      the variable selectors
     * @param valueSelectors         the value selectors
     * @param solutionSaving         condition(s) to apply solution saving
     * @param bestUntilFirstSolution condition(s) to apply best value selection
     */
    public RoundRobin(IntVar[] variables,
                      MetaStrategy<IntVar>[] metaStrategies,
                      VariableSelector<IntVar>[] variableSelectors,
                      IntValueSelector[] valueSelectors,
                      BooleanSupplier solutionSaving,
                      BooleanSupplier bestUntilFirstSolution) {
        this(variables, metaStrategies, variableSelectors, valueSelectors, solutionSaving, bestUntilFirstSolution,
                new Policy.NextModuloN(metaStrategies.length * variableSelectors.length * valueSelectors.length),
                (a, s) -> 0.0);
    }

    /**
     * Creates a RoundRobin strategy.
     * The strategy selects the next variable to branch on using the variable selectors in the order they are given.
     * The strategy selects the next value to assign to the selected variable using the value selectors in the order they are given.
     * The selectors are changed on each restart.
     *
     * @param variables              the variables to branch on
     * @param variableSelectors      the variable selectors
     * @param valueSelectors         the value selectors
     * @param solutionSaving         condition(s) to apply solution saving
     * @param bestUntilFirstSolution condition(s) to apply best value selection
     */
    public RoundRobin(IntVar[] variables,
                      MetaStrategy<IntVar>[] metaStrategies,
                      VariableSelector<IntVar>[] variableSelectors,
                      IntValueSelector[] valueSelectors,
                      BooleanSupplier solutionSaving,
                      BooleanSupplier bestUntilFirstSolution,
                      Policy bandit,
                      ToDoubleBiFunction<Integer, Integer> reward) {
        super(variables);
        this.metaStrategies = metaStrategies;
        this.variableSelectors = variableSelectors;
        this.valueSelectors = valueSelectors;

        int[][] sizes = new int[3][];
        sizes[META] = new int[metaStrategies.length];
        sizes[VARIABLE] = new int[variableSelectors.length];
        sizes[VALUE] = new int[valueSelectors.length];
        combinations = populate(sizes);
        this.action = 0;
        this.step = 0;

        Solution solution = vars[0].getModel().getSolver().defaultSolution();
        solutionFirst = new IntDomainLast(solution, valueSelectors[0],
                (x, v) -> solutionSaving.getAsBoolean());
        bestFirst = new IntDomainBest(valueSelectors[0],
                v -> bestUntilFirstSolution.getAsBoolean());

        this.bandit = bandit;
        this.reward = reward;
    }

    private List<int[]> populate(int[][] sizes) {
        List<int[]> combinations = new ArrayList<>();
        int n = sizes.length;
        int[] tmp = new int[n];
        while (true) {
            // save the current indices
            int[] currentIndices = new int[n];
            System.arraycopy(tmp, 0, currentIndices, 0, n);
            combinations.add(currentIndices);
            int j;
            for (j = 0; j < n; j++) {
                tmp[j]++;
                if (tmp[j] < sizes[j].length) {
                    break;
                }
                tmp[j] = 0;
            }
            if (j == n) {
                break;
            }
        }
        return combinations;
    }

    @Override
    public boolean init() {
        Solver solver = vars[0].getModel().getSolver();
        if (!solver.getSearchMonitors().contains(this)) {
            solver.plugMonitor(this);
        }
        boolean init = true;
        for (MetaStrategy<IntVar> m : metaStrategies) {
            init &= m.init();
        }
        for (VariableSelector<IntVar> vs : variableSelectors) {
            init &= vs.init();
        }
        return init;
    }

    @Override
    public void remove() {
        for (MetaStrategy<IntVar> m : metaStrategies) {
            m.remove();
        }
        for (VariableSelector<IntVar> vs : variableSelectors) {
            vs.remove();
        }
        Solver solver = vars[0].getModel().getSolver();
        if (solver.getSearchMonitors().contains(this)) {
            solver.unplugMonitor(this);
        }
    }

    @Override
    public IntDecision getDecision() {
        int[] currentIndices = combinations.get(action);
        IntDecision decision = null;
        //1. call the meta strategy
        MetaStrategy<IntVar> metaStrategy = metaStrategies[currentIndices[META]];
        IntVar selectedVariable = metaStrategy.getSelectedVariable();
        if (selectedVariable == null) {
            //2. call the strategy, if needed
            VariableSelector<IntVar> strategy = variableSelectors[currentIndices[VARIABLE]];
            selectedVariable = strategy.getVariable(vars);
        }
        if (selectedVariable != null) {
            // 3. apply best value selection, if the condition is met
            OptionalInt opt = bestFirst.apply(selectedVariable);
            if (opt.isEmpty()) {
                //4. apply phase saving, if the condition is met
                opt = solutionFirst.apply(selectedVariable);
            }
            //5. call the value selector
            if (opt.isEmpty()) {
                IntValueSelector valueSelector = valueSelectors[currentIndices[VALUE]];
                opt = OptionalInt.of(valueSelector.selectValue(selectedVariable));
            }
            decision = vars[0].getModel().getSolver().getDecisionPath()
                    .makeIntDecision(selectedVariable, DecisionOperatorFactory.makeIntEq(), opt.getAsInt());
        }
        return decision;
    }

    @Override
    public void afterRestart() {
        step++;
        bandit.update(action, reward.applyAsDouble(action, step));
        action = bandit.nextAction(step);
        /*System.out.printf("c Action %d : [%s, %s, %s]\n",
                action,
                metaStrategies[combinations.get(action)[META]].getClass().getSimpleName(),
                variableSelectors[combinations.get(action)[VARIABLE]].getClass().getSimpleName(),
                valueSelectors[combinations.get(action)[VALUE]].getClass().getSimpleName());*/
    }

}
