/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver;

import java.util.*;
import java.util.function.Function;

import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.ConstraintsName;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.nary.cumulative.*;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.search.strategy.strategy.SetTimes;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;

/**
 * Interface to make and declare everything useful for scheduling problems (Task objects, constraints, search heuristics, etc.)
 *
 * @author Arthur GODET <arth.godet@gmail.com>
 * @since 17/08/2021
 */
public interface ISchedulingFactory extends ISelf<Model> {

    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////// TASKS ///////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param model the Model of the variables
     * @param est   earliest starting time
     * @param lst   latest starting time
     * @param d     duration
     * @param ect   earliest completion time
     * @param lct   latest completion time time
     */
    default Task task(Model model, int est, int lst, int d, int ect, int lct) {
        return new Task(model, est, lst, d, ect, lct);
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     */
    default Task task(IntVar s, int d) {
        return new Task(s, d);
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     * @param e end variable
     */
    default Task task(IntVar s, int d, IntVar e) {
        return new Task(s, d, e);
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end
     *
     * @param s start variable
     * @param d duration variable
     * @param e end variable
     */
    default Task task(IntVar s, IntVar d, IntVar e) {
        return new Task(s, d, e);
    }

    /////////////////////////////////////////////////////////////////////////////
    //////////////////////////////// DISJUNCTIVE ////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a disjunctive constraint: Enforces that no two tasks are concurrently done (even if one has duration of
     * size zero).
     *
     * @param tasks Task objects containing start, duration and end variables
     * @return a cumulative constraint
     */
    default Constraint disjunctive(Collection<Task> tasks) {
        return disjunctive(tasks.toArray(new Task[0]));
    }

    /**
     * Creates a disjunctive constraint: Enforces that no two tasks are concurrently done (even if one has duration of
     * size zero).
     *
     * @param tasks Task objects containing start, duration and end variables
     * @return a cumulative constraint
     */
    default Constraint disjunctive(Task[] tasks) {
        if (tasks.length >= 2) {
            final Propagator<IntVar> propDisjunctive;
            if (tasks.length == 2) {
                propDisjunctive = new PropDisjunctiveTwoTasks(tasks[0], tasks[1]);
            } else {
                final IntVar capacity = tasks[0].getModel().intVar(1);
                final IntVar[] heights = new IntVar[tasks.length];
                Arrays.fill(heights, capacity);
                propDisjunctive = new PropagatorDisjunctive(tasks, heights, capacity);
            }
            return new Constraint(
                    ConstraintsName.DISJUNCTIVE,
                    propDisjunctive
            );
        } else {
            return ref().trueConstraint();
        }
    }

    /**
     * Creates a disjunctive constraint: <ul>
     *     <li>no two tasks are concurrently done (even if one has duration of size zero) iff their associated height
     *     is strictly greater than zero</li>
     *     <li>height variables are greater than zero</li>
     *     <li>capacity is greater than any height variable</li>
     * </ul>
     *
     * @param tasks    Task objects containing start, duration and end variables
     * @param heights  integer variables representing the resource consumption of each task
     * @param capacity integer variable representing the resource capacity
     * @return a cumulative constraint
     */
    default Constraint disjunctive(List<Task> tasks, List<IntVar> heights, IntVar capacity) {
        if (tasks.size() != heights.size()) {
            throw new SolverException("Tasks and heights arrays should have same size");
        }
        Task[] tasksArray = new Task[tasks.size()];
        IntVar[] heightsArray = new IntVar[tasks.size()];
        for (int i = 0; i < tasks.size(); ++i) {
            tasksArray[i] = tasks.get(i);
            heightsArray[i] = heights.get(i);
        }
        return disjunctive(tasksArray, heightsArray, capacity);
    }

    /**
     * Creates a disjunctive constraint: <ul>
     *     <li>no two tasks are concurrently done (even if one has duration of size zero) iff their associated height
     *     is strictly greater than zero</li>
     *     <li>height variables are greater than zero</li>
     *     <li>capacity is greater than any height variable</li>
     * </ul>
     *
     * @param tasks    Task objects containing start, duration and end variables
     * @param heights  integer variables representing the resource consumption of each task
     * @param capacity integer variable representing the resource capacity
     * @return a cumulative constraint
     */
    default Constraint disjunctive(Task[] tasks, IntVar[] heights, IntVar capacity) {
        if (tasks.length != heights.length) {
            throw new SolverException("Tasks and heights arrays should have same size");
        }
        if (tasks.length >= 2) {
            final Propagator<IntVar> propDisjunctive;
            if (tasks.length == 2) {
                propDisjunctive = new PropDisjunctiveTwoTasks(tasks[0], heights[0], tasks[1], heights[1]);
            } else {
                propDisjunctive = new PropagatorDisjunctive(tasks, heights, capacity);
            }
            return new Constraint(
                    ConstraintsName.DISJUNCTIVE,
                    new PropagatorCapacity(tasks, heights, capacity),
                    propDisjunctive
            );
        } else {
            return ref().trueConstraint();
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    //////////////////////////////// CUMULATIVE /////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a cumulative constraint: Enforces that at each point in time,
     * the cumulated height of the set of tasks that overlap that point
     * does not exceed a given limit.
     * <p>
     * heights should be >= 0
     * Discards tasks whose duration or height is equal to zero
     *
     * @param tasks    Task objects containing start, duration and end variables
     * @param heights  integer variables representing the resource consumption of each task
     * @param capacity integer variable representing the resource capacity
     * @return a cumulative constraint
     */
    default Constraint cumulative(List<Task> tasks, List<IntVar> heights, IntVar capacity) {
        if (tasks.size() != heights.size()) {
            throw new SolverException("Tasks and heights arrays should have same size");
        }
        Task[] tasksArray = new Task[tasks.size()];
        IntVar[] heightsArray = new IntVar[tasks.size()];
        for (int i = 0; i < tasks.size(); ++i) {
            tasksArray[i] = tasks.get(i);
            heightsArray[i] = heights.get(i);
        }
        return cumulative(tasksArray, heightsArray, capacity);
    }

    /**
     * Creates a cumulative constraint: Enforces that at each point in time,
     * the cumulated height of the set of tasks that overlap that point
     * does not exceed a given limit.
     * <p>
     * heights should be >= 0
     * Discards tasks whose duration or height is equal to zero
     *
     * @param tasks    Task objects containing start, duration and end variables
     * @param heights  integer variables representing the resource consumption of each task
     * @param capacity integer variable representing the resource capacity
     * @return a cumulative constraint
     */
    default Constraint cumulative(Task[] tasks, IntVar[] heights, IntVar capacity) {
        if (tasks.length != heights.length) {
            throw new SolverException("Tasks and heights arrays should have same size");
        }
        // keep only tasks that can have impact on resource consumption
        final List<Task> tasksToKeep = new ArrayList<>();
        final List<IntVar> heightsToKeep = new ArrayList<>();
        for (int i = 0; i < heights.length; i++) {
            if (heights[i].getUB() > 0 && tasks[i].getMaxDuration() > 0) {
                tasksToKeep.add(tasks[i]);
                heightsToKeep.add(heights[i]);
            }
        }
        if (tasksToKeep.isEmpty()) {
            return ref().arithm(capacity, ">=", 0);
        }
        final Task[] keptTasks = new Task[tasksToKeep.size()];
        final IntVar[] keptHeights = new IntVar[tasksToKeep.size()];
        for (int i = 0; i < tasksToKeep.size(); ++i) {
            keptTasks[i] = tasksToKeep.get(i);
            keptHeights[i] = heightsToKeep.get(i);
        }
        if (keptTasks.length <= 1) {
            return ref().arithm(keptHeights[0], "<=", capacity);
        } else if (capacity.getUB() <= 1) {
            return ref().disjunctive(keptTasks, keptHeights, capacity);
        } else {
            final List<Task> tasksInDisjunctive = new ArrayList<>();
            final List<IntVar> heightsInDisjunctive = new ArrayList<>();
            final List<Propagator<IntVar>> propagators = new ArrayList<>();
            propagators.add(new PropagatorCapacity(keptTasks, keptHeights, capacity));
            int idxTaskWithGreaterHeight = -1;
            int smallestHeightValueGreaterHeight = 0;
            int minHeightsInDisjunctive = Integer.MAX_VALUE;
            for (int i = 0; i < keptTasks.length; ++i) {
                // get the smallest value of height strictly greater than 0, i.e. the smallest height value if the task
                // does count in the cumulative
                final int smallestHeightI = keptHeights[i].stream().filter(v -> v > 0).min().orElse(0);
                if (2 * smallestHeightI > capacity.getUB()) {
                    tasksInDisjunctive.add(keptTasks[i]);
                    heightsInDisjunctive.add(keptHeights[i]);
                    minHeightsInDisjunctive = Math.min(minHeightsInDisjunctive, smallestHeightI);
                } else if (idxTaskWithGreaterHeight == -1 || smallestHeightI > smallestHeightValueGreaterHeight) {
                    idxTaskWithGreaterHeight = i;
                    smallestHeightValueGreaterHeight = smallestHeightI;
                }
            }
            if (!tasksInDisjunctive.isEmpty()) {
                if (idxTaskWithGreaterHeight != -1
                        && smallestHeightValueGreaterHeight + minHeightsInDisjunctive > capacity.getUB()) {
                    tasksInDisjunctive.add(keptTasks[idxTaskWithGreaterHeight]);
                    heightsInDisjunctive.add(keptHeights[idxTaskWithGreaterHeight]);
                }
                if (tasksInDisjunctive.size() >= 2) {
                    final Task[] tasksDisjunctive = new Task[tasksInDisjunctive.size()];
                    final IntVar[] heightsDisjunctive = new IntVar[tasksInDisjunctive.size()];
                    for (int i = 0; i < tasksInDisjunctive.size(); ++i) {
                        tasksDisjunctive[i] = tasksInDisjunctive.get(i);
                        heightsDisjunctive[i] = heightsInDisjunctive.get(i);
                    }
                    final Propagator<IntVar> propDisjunctive;
                    if (tasksDisjunctive.length == 2) {
                        propDisjunctive = new PropDisjunctiveTwoTasks(
                                tasksDisjunctive[0],
                                heightsDisjunctive[0],
                                tasksDisjunctive[1],
                                heightsDisjunctive[1]
                        );
                    } else {
                        propDisjunctive = new PropagatorDisjunctive(tasksDisjunctive, heightsDisjunctive, capacity);
                    }
                    propagators.add(propDisjunctive);
                }
            }
            if (tasksInDisjunctive.size() < keptTasks.length) {
                propagators.add(new PropagatorCumulative(keptTasks, keptHeights, capacity));
            }
            return new Constraint(
                    ConstraintsName.CUMULATIVE,
                    propagators.toArray(new Propagator[0])
            );
        }
    }

    /**
     * Creates a cumulative constraint:
     * Enforces that at each point in time,
     * the cumulated height of the set of tasks that overlap that point
     * does not exceed a given limit.
     * <p>
     * Task duration and height should be >= 0
     * Discards tasks whose duration or height is equal to zero
     *
     * @param starts    starting time of each task
     * @param durations processing time of each task
     * @param heights   resource consumption of each task
     * @param capacity  resource capacity
     */
    default Constraint cumulative(IntVar[] starts, int[] durations, int[] heights, int capacity) {
        int n = starts.length;
        final IntVar[] d = new IntVar[n];
        final IntVar[] h = new IntVar[n];
        final IntVar[] e = new IntVar[n];
        Task[] tasks = new Task[n];
        for (int i = 0; i < n; i++) {
            d[i] = ref().intVar(durations[i]);
            h[i] = ref().intVar(heights[i]);
            e[i] = ref().intVar(starts[i].getName() + "_e",
                    starts[i].getLB() + durations[i],
                    starts[i].getUB() + durations[i],
                    true);
            tasks[i] = new Task(starts[i], d[i], e[i]);
        }
        return cumulative(tasks, h, ref().intVar(capacity));
    }

    /////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////// SEARCH ///////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////

    default IntVar[] extractVars(Task[] tasks, Function<Task, IntVar> function) {
        return Arrays.stream(tasks).map(function).toArray(IntVar[]::new);
    }

    default IntVar[] extractStartVars(Task[] tasks) {
        return extractVars(tasks, Task::getStart);
    }

    default IntVar[] extractDurationVars(Task[] tasks) {
        return extractVars(tasks, Task::getDuration);
    }

    default IntVar[] extractEndVars(Task[] tasks) {
        return extractVars(tasks, Task::getEnd);
    }

    /**
     * Search for scheduling problems described in the following paper :
     * Godard, D., Laborie, P., Nuijten, W.: Randomized large neighborhood search for cumulative scheduling. In: Biundo, S., Myers, K.L., Rajan, K. (eds.) Proceedings of the Fifteenth International Conference on Automated Planning and Scheduling(ICAPS 2005), June 5-10 2005, Monterey, California, USA. pp. 81–89. AAAI (2005),http://www.aaai.org/Library/ICAPS/2005/icaps05-009.php
     * <p>
     * Beware that the search is not complete in general, as stated in the paper.
     *
     * @param tasks the tasks
     * @return the strategy
     */
    default IntStrategy setTimes(Task[] tasks) {
        SetTimes setTimes = new SetTimes(tasks);
        ref().post(new Constraint("SET_TIMES", setTimes));
        return Search.intVarSearch(setTimes, new IntDomainMin(), extractStartVars(tasks));
    }

    enum ArbitrationRule {
        MIN_EST, MAX_EST, MIN_LST, MAX_LST, MIN_ECT, MAX_ECT, MIN_LCT, MAX_LCT
    }

    /**
     * Returns true if the first task is before the second task according to the rule.
     * If the rule is MIN_EST(0), then returns task1.est < task2.est
     * If the rule is MAX_EST(1), then returns task1.est > task2.est
     * If the rule is MIN_LST(2), then returns task1.lst < task2.lst
     * If the rule is MAX_LST(3), then returns task1.lst > task2.lst
     * If the rule is MIN_ECT(4), then returns task1.eet < task2.eet
     * If the rule is MAX_ECT(5), then returns task1.eet > task2.eet
     * If the rule is MIN_LCT(6), then returns task1.let < task2.let
     * If the rule is MAX_LCT(7), then returns task1.let > task2.let
     *
     * @param task1 the first task
     * @param task2 the second task
     * @param rule  the rule
     * @return true iff task1 is before task2 according to the rule
     */
    default boolean before(Task task1, Task task2, ArbitrationRule rule) {
        switch (rule) {
            case MIN_EST:
                return task1.getStart().getLB() < task2.getStart().getLB();
            case MAX_EST:
                return task1.getStart().getLB() > task2.getStart().getLB();
            case MIN_LST:
                return task1.getStart().getUB() < task2.getStart().getUB();
            case MAX_LST:
                return task1.getStart().getUB() > task2.getStart().getUB();
            case MIN_ECT:
                return task1.getEnd().getLB() < task2.getEnd().getLB();
            case MAX_ECT:
                return task1.getEnd().getLB() > task2.getEnd().getLB();
            case MIN_LCT:
                return task1.getEnd().getUB() < task2.getEnd().getUB();
            case MAX_LCT:
                return task1.getEnd().getUB() > task2.getEnd().getUB();
            default:
                throw new UnsupportedOperationException("Task comparison should be either MIN_EST(0), MAX_EST(1), MIN_LST(2), MAX_LST(3), MIN_EET(4), MAX_EET(5), MIN_LET(6) or MAX_LET(7).");
        }
    }

    /**
     * Builds a strategy that selects the task's start with the smallest lower bound (in case of equality, it selects the task with the smallest end's lower bound).
     *
     * @param tasks the tasks
     * @return the strategy
     */
    default IntStrategy smallest(Task[] tasks) {
        return smallest(tasks, ArbitrationRule.MIN_ECT);
    }

    default IntStrategy smallest(Task[] tasks, ArbitrationRule arbitrationRule) {
        IntVar[] starts = extractStartVars(tasks);
        return Search.intVarSearch(
                variables -> {
                    int idx = -1;
                    for (int i = 0; i < tasks.length; i++) {
                        if (!starts[i].isInstantiated()) {
                            if (idx == -1
                                    || starts[i].getLB() < starts[idx].getLB()
                                    || starts[i].getLB() == starts[idx].getLB() && before(tasks[i], tasks[idx], arbitrationRule)
                            ) {
                                idx = i;
                            }
                        }
                    }
                    return idx == -1 ? null : starts[idx];
                },
                new IntDomainMin(),
                starts
        );
    }
}
