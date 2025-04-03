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

import gnu.trove.map.hash.TIntIntHashMap;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Propagator for the Cumulative constraint.
 * It uses : <ul>
 * <li>the scalable TimeTable algorithm from Gay et al. [Gay2015]</li>
 * <li>the OverloadChecking algorithm from Vilim [Vilim2011]</li>
 * </ul>
 *
 * <a href="https://doi.org/10.1007/978-3-319-23219-5_11">Gay, S., Hartert, R., and Schaus, P.: “Simple and Scalable Time-Table Filtering for the Cumulative Constraint”. In: Principles and Practice of Constraint Programming - 21st International Conference, CP 2015, Cork, Ireland, August 31 - September 4, 2015, Proceedings. Ed. by Gilles Pesant. Vol. 9255. Lecture Notes in Computer Science. Springer, 2015, pp. 149–157</a>
 * <br>
 * <a href="https://doi.org/10.1007/978-3-642-21311-3_22">Petr Vilım: “Timetable Edge Finding Filtering Algorithm for Discrete Cumulative Resources”. In: Integration of AI and OR Techniques in Constraint Programming for Combinatorial Optimization Problems - 8th International Conference, CPAIOR 2011, Berlin, Germany, May 23-27, 2011. Proceedings. Ed. by Tobias Achterberg and J. Christopher Beck. Vol. 6697. Lecture Notes in Computer Science. Springer, 2011, pp. 230–245</a>
 *
 * @author Arthur Godet <arth.godet@gmail.com>
 * @since 19/10/2023
 */
public class PropagatorCumulative extends PropagatorResource {
    private static int getFreeDuration(Task task) {
        int pTT = Math.max(0, task.getEct() - task.getLst());
        return task.getDuration().getLB() - pTT;
    }

    private static int compareTaskWithFreeParts(List<Task> tasks, int i, int j) {
        if (tasks.get(i).getEst() == tasks.get(j).getEst()) {
            return tasks.get(i).getEst() + getFreeDuration(tasks.get(i))
                   - (tasks.get(j).getEst() + getFreeDuration(tasks.get(j)));
        }
        return tasks.get(i).getEst() - tasks.get(j).getEst();
    }

    protected final Profile profile;
    // Pour l'overloadChecking
    protected final TIntIntHashMap ttAfter;
    protected final List<Integer> tasksWithFreeParts;

    ////////////////////////////////////////////////////////////////////
    ////////////////        CUMULATIVE FILTERING        ////////////////
    ////////////////////////////////////////////////////////////////////
    private final int[] array = new int[2];

    public PropagatorCumulative(Task[] tasks, IntVar[] heights, IntVar capacity) {
        super(false, tasks, heights, capacity, PropagatorPriority.QUADRATIC, true);
        profile = new Profile(tasks.length);
        tasksWithFreeParts = new ArrayList<>(tasks.length);
        ttAfter = new TIntIntHashMap(2 * tasks.length);
    }

    protected void scalableTimeTable() throws ContradictionException {
        boolean hasFiltered;
        int maxHeight;
        do {
            profile.buildProfile(performedTasks, tasksHeights);
            maxHeight = profile.getHeightRectangle(0);
            for (int j = 1; j < profile.size(); j++) {
                maxHeight = Math.max(maxHeight, profile.getHeightRectangle(j));
            }
            capacity.updateLowerBound(maxHeight, this);
            hasFiltered = scalableTimeTable(performedAndOptionalTasks, tasksHeightsWithOptional);
            updateHeights(performedAndOptionalTasks, tasksHeightsWithOptional);
            if (hasFiltered) {
                enforceTaskVariablesRelation(performedAndOptionalTasks);
            }
        } while (hasFiltered);
    }

    protected void updateHeights(List<Task> tasks, List<IntVar> heights) throws ContradictionException {
        for (int i = 0; i < tasks.size(); i++) {
            final Task task = tasks.get(i);
            if (task.mustBePerformed()) {
                final IntVar height = heights.get(i);
                int j = profile.find(task.getEct() - task.getMinDuration());
                int minRectangleHeight = capacity.getUB();
                boolean shouldFilter = false;
                while (j < profile.size() && profile.getStartRectangle(j) < task.getLst() + task.getMinDuration()) {
                    if (task.hasCompulsoryPart()
                        && task.getLst() < profile.getEndRectangle(j)
                        && profile.getStartRectangle(j) < task.getEct()) {
                        height.updateUpperBound(capacity.getUB() - (profile.getHeightRectangle(j) - height.getLB()), this);
                    } else {
                        minRectangleHeight = Math.min(profile.getHeightRectangle(j), minRectangleHeight);
                        shouldFilter = true;
                    }
                    j++;
                }
                if (shouldFilter) {
                    height.updateUpperBound(capacity.getUB() - minRectangleHeight, this);
                }
            }
        }
    }

    protected boolean scalableTimeTable(List<Task> tasks, List<IntVar> heights) throws ContradictionException {
        // From PropCumulativeGay2015
        boolean hasFiltered = false;
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            hasFiltered |= scalableTimeTableFilterEst(task, heights.get(i));
            hasFiltered |= scalableTimeTableFilterLct(task, heights.get(i));
        }
        return hasFiltered;
    }

    protected boolean scalableTimeTableFilterEst(Task task, IntVar height) throws ContradictionException {
        boolean hasFiltered = false;
        if (!task.getStart().isInstantiated()) {
            int j = profile.find(task.getEst());
            while (j < profile.size() && profile.getStartRectangle(j) < Math.min(task.getEct(), task.getLst())) {
                if (capacity.getUB() - height.getLB() < profile.getHeightRectangle(j)) {
                    hasFiltered |= PropagatorResource.filterEst(task, height, Math.min(task.getLst(), profile.getEndRectangle(j)), this)
                                   && PropagatorResource.mustBePerformed(task, height);
                }
                j++;
            }
        }
        return hasFiltered;
    }

    protected boolean scalableTimeTableFilterLct(Task task, IntVar height) throws ContradictionException {
        boolean hasFiltered = false;
        if (!task.getEnd().isInstantiated()) {
            int j = profile.find(task.getLct() - 1);
            while (j >= 1 && profile.getEndRectangle(j) > Math.max(task.getLst(), task.getEct())) {
                if (capacity.getUB() - height.getLB() < profile.getHeightRectangle(j)) {
                    hasFiltered |= PropagatorResource.filterLct(task, height, Math.max(profile.getStartRectangle(j), task.getEct()), this)
                                   && PropagatorResource.mustBePerformed(task, height);
                }
                j--;
            }
        }
        return hasFiltered;
    }

    protected void computeTtAfter(List<Task> tasks) {
        ttAfter.clear();
        for (int i = 0; i < tasks.size(); ++i) {
            final int est = tasks.get(i).getEst();
            final int lct = tasks.get(i).getLct();
            if (!ttAfter.containsKey(est) || !ttAfter.containsKey(lct)) {
                computeTtAfter(est, lct);
            }
        }
    }

    protected void computeTtAfter(final int est, final int lct) {
        int ttAfterTime = 0;
        int idx = profile.size() - 1;
        while (0 <= idx && lct <= profile.getStartRectangle(idx)) {
            ttAfterTime += profile.getHeightRectangle(idx) * (profile.getEndRectangle(idx) - profile.getStartRectangle(idx));
            idx--;
        }
        if (0 <= idx && lct < profile.getEndRectangle(idx)) {
            ttAfter.put(lct, ttAfterTime + profile.getHeightRectangle(idx) * (profile.getEndRectangle(idx) - lct));
        } else {
            ttAfter.put(lct, ttAfterTime);
        }
        while (0 <= idx && est <= profile.getStartRectangle(idx)) {
            ttAfterTime += profile.getHeightRectangle(idx) * (profile.getEndRectangle(idx) - profile.getStartRectangle(idx));
            idx--;
        }
        if (0 <= idx && est < profile.getEndRectangle(idx)) {
            ttAfter.put(est, ttAfterTime + profile.getHeightRectangle(idx) * (profile.getEndRectangle(idx) - est));
        } else {
            ttAfter.put(est, ttAfterTime);
        }
    }

    protected void computeTasksWithFreeParts(List<Task> tasks) {
        tasksWithFreeParts.clear();
        for (int i = 0; i < tasks.size(); i++) {
            if (getFreeDuration(tasks.get(i)) > 0) {
                tasksWithFreeParts.add(i);
            }
        }
        // Sort free parts
        tasksWithFreeParts.sort((i, j) -> compareTaskWithFreeParts(tasks, i, j));
    }

    ////////////////////////////////////////////////////////////////////
    ////////////////            PROPAGATION             ////////////////
    ////////////////////////////////////////////////////////////////////

    protected void overloadChecking(List<Task> tasks, List<IntVar> heights) throws ContradictionException {
        // From PropCumulativeVilim2011
        computeTtAfter(tasks);
        computeTasksWithFreeParts(tasks);
        int eEF;
        int a;
        int b;
        int lctB;
        for (int i = 0; i < tasksWithFreeParts.size(); i++) {
            b = tasksWithFreeParts.get(i);
            lctB = tasks.get(b).getLct();
            eEF = 0;
            for (int k = tasksWithFreeParts.size() - 1; k >= 0; k--) {
                a = tasksWithFreeParts.get(k);
                if (tasks.get(a).getLct() <= tasks.get(b).getLct() && PropagatorResource.mustBePerformed(tasks.get(a), heights.get(a))) {
                    eEF += getFreeDuration(tasks.get(a)) * heights.get(a).getLB();
                    if (capacity.getUB() * (lctB - tasks.get(a).getEst()) < eEF + ttAfter.get(tasks.get(a).getEst()) - ttAfter.get(lctB)) {
                        PropagatorResource.filterOptionalTask(tasks.get(b), heights.get(b), this);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        computeMustBePerformedTasks();
        scalableTimeTable();
        overloadChecking(performedTasks, tasksHeights);
    }
}
