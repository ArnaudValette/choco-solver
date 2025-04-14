/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.cumulative;

import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;

import java.util.*;

/**
 * Propagator for the Disjunctive constraint.
 * It uses : <ul>
 * <li>the Edge-Finding and Immediate Selections from Carlier et al. [Carlier1994]</li>
 * <li>the OverloadChecking, Detectable Precedences and Not-First/Not-Last algorithms from Vilim. [Vilim2004]</li>
 * </ul>
 *
 * <a href="https://doi.org/10.1016/0377-2217(94)90379-4">Carlier, J., Pinson, E.: Adjustment of heads and tails for the job-shop problem. In: European Journal of Operational Research, Volume 78, pp. 146-161 (1994).</a>
 * <br>
 * <a href="https://doi.org/10.1007/978-3-540-24664-0_23">Vilim, P.: O(n log(n)) Filtering Algorithms for Unary Resource Constraint. In: Integration of AI and OR Techniques in Constraint Programming for Combinatorial Optimization Problems, First International Conference, CPAIOR 2004, Nice, France, April 20-22, 2004, Proceedings. Ed. by Jean-Charles Régin and Michel Rueher. Vol. 3011. Lecture Notes in Computer Science. Springer, 2004, pp. 335–34 (2004).</a>
 *
 * @author Arthur Godet <arth.godet@gmail.com>
 * @since 18/03/2025
 */
public class PropagatorDisjunctive extends PropagatorResource {
    public static final boolean MANAGE_OPTIONALITY = false;
    // For algorithms of Vilim2004
    private final List<Integer> queue;
    private final ThetaTree tree;
    private final List<Integer> queue2;
    private final List<Integer> indexes2;
    private final ThetaTree tree2;
    // For algorithms of Carlier1994
    private final ArrayList<Integer> l1;
    private final ArrayList<Integer> l2;
    private final Map<Integer, Integer> newEst;
    private final Map<Integer, Integer> newLct;
    private final AscendantSetSearchTree ascendantSetSearchTree;
    private final AscendantSetSearchTree ascendantSetSearchTree2;

    public PropagatorDisjunctive(final Task[] tasks, final IntVar[] heights, final IntVar capacity) {
        super(true, tasks, heights, capacity, PropagatorPriority.QUADRATIC, false);
        // For Carlier1994
        l1 = new ArrayList<>(tasks.length);
        l2 = new ArrayList<>(tasks.length);
        newEst = new HashMap<>(tasks.length);
        newLct = new HashMap<>(tasks.length);
        ascendantSetSearchTree = new AscendantSetSearchTree(tasks.length);
        // For Vilim2004
        queue = new ArrayList<>(tasks.length);
        tree = new ThetaTree(tasks.length);

        ascendantSetSearchTree2 = new AscendantSetSearchTree(tasks.length);
        queue2 = new ArrayList<>(tasks.length);
        indexes2 = new ArrayList<>(tasks.length);
        tree2 = new ThetaTree(tasks.length);
    }

    private boolean immediateSelections(final List<Task> tasks) throws ContradictionException {
        if (tasks.isEmpty()) {
            return false;
        }
        l1.clear();
        l2.clear();
        for (int i = 0; i < tasks.size(); i++) {
            l1.add(i);
            l2.add(i);
        }
        l1.sort(Comparator.comparingInt(i -> -tasks.get(i).getLst())); // sort by decreasing lst
        l2.sort(Comparator.comparingInt(i -> -tasks.get(i).getEct())); // sort by decreasing ect
        int i1 = 0;
        int i2 = 0;

        boolean hasFiltered = false;
        Task t1 = tasks.get(l1.get(i1));
        Task t2 = tasks.get(l2.get(i2));
        newEst.clear();
        newLct.clear();
        while (i1 < l1.size() && i2 < l2.size()) {
            if (t1.equals(t2) || t2.getEst() + t2.getMinDuration() + t1.getMinDuration() <= t1.getLct()) { // t2.ect <= t1.lst
                // Update i1
                i1++;
                if (i1 < l1.size()) {
                    t1 = tasks.get(l1.get(i1));
                }
            } else {
                // task at i2 is after all tasks at i1 and after
                for (int j = i1; j < l1.size(); j++) {
                    // tj.end <= t2.start
                    if (l1.get(j) != l2.get(i2)) {
                        newEst.putIfAbsent(l2.get(i2), tasks.get(l1.get(j)).getEct());
                        newEst.put(l2.get(i2), Math.max(newEst.get(l2.get(i2)), tasks.get(l1.get(j)).getEct()));
                        newLct.putIfAbsent(l1.get(j), t2.getLst());
                        newLct.put(l1.get(j), Math.min(newLct.get(l1.get(j)), t2.getLst()));
                    }
                }
                // Update i2
                i2++;
                if (i2 < l2.size()) {
                    t2 = tasks.get(l2.get(i2));
                }
            }
        }
        for (Map.Entry<Integer, Integer> entry : newEst.entrySet()) {
            hasFiltered |= tasks.get(entry.getKey()).updateEst(entry.getValue(), this);
        }
        for (Map.Entry<Integer, Integer> entry : newLct.entrySet()) {
            hasFiltered |= tasks.get(entry.getKey()).updateLct(entry.getValue(), this);
        }
        return hasFiltered;
    }

    private boolean edgeFinding(final List<Task> tasks, final AscendantSetSearchTree ascendantSetSearchTree) throws ContradictionException {
        return ascendantSetSearchTree.adjust(tasks, this);
    }

    @Override
    protected void recomputeDataStructure() {
        if (queue.size() != performedTasks.size()) {
            queue.clear();
            queue2.clear();
            indexes2.clear();
            for (int i = 0; i < performedTasks.size(); i++) {
                queue.add(i);
                queue2.add(i);
                indexes2.add(i);
            }
        }
    }

    private void overloadChecking() throws ContradictionException {
        tree.init(performedTasks);
        indexes.sort(Comparator.comparingInt(i -> performedTasks.get(i).getLct()));
        for (int k = 0; k < indexes.size(); k++) {
            int id = indexes.get(k);
            tree.add(id);
            if (tree.getEct() > performedTasks.get(id).getLct()) {
                fails();
            }
        }
    }

    private boolean notFirstNotLast(
            final List<Task> tasks,
            final ThetaTree tree,
            final List<Integer> queue,
            final List<Integer> indexes
    ) throws ContradictionException {
        tree.init(tasks);
        queue.sort(Comparator.comparingInt(i -> tasks.get(i).getLst()));
        int q = 0;
        indexes.sort(Comparator.comparingInt(i -> tasks.get(i).getLct()));
        boolean hasFiltered = false;
        for (int k = 0; k < indexes.size(); k++) {
            final int id = indexes.get(k);
            while (q < queue.size() && tasks.get(id).getLct() > tasks.get(queue.get(q)).getLst()) {
                tree.add(queue.get(q++));
            }
            if (tree.isPresent(id) && tree.getEctWithout(id) > tasks.get(id).getLst()) {
                hasFiltered |= tasks.get(id).updateLct(tasks.get(queue.get(q - 1)).getLst(), this);
            }
        }
        return hasFiltered;
    }

    private boolean detectablePrecedences(
            final List<Task> tasks,
            final ThetaTree tree,
            final List<Integer> queue,
            final List<Integer> indexes
    ) throws ContradictionException {
        boolean hasFiltered = false;
        tree.init(tasks);
        queue.sort(Comparator.comparingInt(i -> tasks.get(i).getLst()));
        int q = 0;
        indexes.sort(Comparator.comparingInt(i -> tasks.get(i).getEct()));
        for (int k = 0; k < indexes.size(); k++) {
            final int id = indexes.get(k);
            while (q < queue.size() && tasks.get(id).getEct() > tasks.get(queue.get(q)).getLst()) {
                tree.add(queue.get(q++));
            }
            int ect = tree.getEct();
            if (tree.isPresent(id)) {
                ect = tree.getEctWithout(id);
            }
            hasFiltered |= tasks.get(id).updateEst(ect, this);
        }
        return hasFiltered;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        computeMustBePerformedTasks();
        boolean hasFiltered = true;
        while (hasFiltered) {
            while (hasFiltered) {
                while (hasFiltered) {
                    while (hasFiltered) {
                        overloadChecking();
                        hasFiltered = detectablePrecedences(performedTasks, tree, queue, indexes)
                                      || detectablePrecedences(performedMirrorTasks, tree2, queue2, indexes2);
                        if (hasFiltered) {
                            enforceTaskVariablesRelation();
                        }
                    }
                    hasFiltered = notFirstNotLast(performedTasks, tree, queue, indexes)
                                  || notFirstNotLast(performedMirrorTasks, tree2, queue2, indexes2);
                    if (hasFiltered) {
                        enforceTaskVariablesRelation();
                    }
                }
                hasFiltered = edgeFinding(performedTasks, ascendantSetSearchTree) || edgeFinding(performedMirrorTasks, ascendantSetSearchTree2);
                if (hasFiltered) {
                    enforceTaskVariablesRelation();
                }
            }
            hasFiltered = immediateSelections(performedTasks);
            if (hasFiltered) {
                enforceTaskVariablesRelation();
            }
        }
    }
}
