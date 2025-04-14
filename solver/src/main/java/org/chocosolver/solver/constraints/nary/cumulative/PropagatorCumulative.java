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

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.events.PropagatorEventType;

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

    protected final BacktrackableProfile profile;
    // Pour l'overloadChecking
    protected final TIntIntHashMap ttAfterMap;
    protected final List<Integer> tasksWithFreeParts;
    protected final IStateInt sizeTtAfter;
    protected final IStateInt[] timeValues;
    protected final IStateInt[] ttAfter;
    protected final TIntHashSet set;
    protected final TIntArrayList list;
    protected boolean shouldRecomputeTimeTable = false;
    protected boolean hasRecomputedTimeTable = false;

    public PropagatorCumulative(Task[] tasks, IntVar[] heights, IntVar capacity) {
        super(false, tasks, heights, capacity, PropagatorPriority.QUADRATIC, true, true);
        profile = new BacktrackableProfile(tasks.length, getModel());
        tasksWithFreeParts = new ArrayList<>(tasks.length);
        ttAfterMap = new TIntIntHashMap(2 * tasks.length);
        sizeTtAfter = getModel().getEnvironment().makeInt(0);
        timeValues = new IStateInt[2 * tasks.length];
        ttAfter = new IStateInt[2 * tasks.length];
        for (int i = 0; i < ttAfter.length; ++i) {
            timeValues[i] = getModel().getEnvironment().makeInt(0);
            ttAfter[i] = getModel().getEnvironment().makeInt(0);
        }
        set = new TIntHashSet(2 * tasks.length);
        list = new TIntArrayList(2 * tasks.length);
    }

    protected void scalableTimeTable(final List<Task> tasks, final List<IntVar> heights) throws ContradictionException {
        boolean hasFiltered;
        do {
            if (shouldRecomputeTimeTable) {
                buildProfile(tasks, heights);
                shouldRecomputeTimeTable = false;
                hasRecomputedTimeTable = true;
            }
            hasFiltered = scalableTimeTableFilter(tasks, heights);
        } while (hasFiltered);
    }

    protected void buildProfile(final List<Task> tasks, final List<IntVar> heights) throws ContradictionException {
        profile.buildProfile(tasks, heights);
        int maxHeight = profile.getHeightRectangle(0);
        for (int j = 1; j < profile.size(); j++) {
            maxHeight = Math.max(maxHeight, profile.getHeightRectangle(j));
        }
        capacity.updateLowerBound(maxHeight, this);
    }

    protected void updateHeights(final List<Task> tasks, final List<IntVar> heights) throws ContradictionException {
        for (int i = 0; i < tasks.size(); i++) {
            final Task task = tasks.get(i);
            if (task.getLst() < task.getEct() && task.mustBePerformed()) {
                final IntVar height = heights.get(i);
                int j = profile.find(task.getLst());
                while (j < profile.size() && profile.getStartRectangle(j) < task.getEct()) {
                    height.updateUpperBound(capacity.getUB() - (profile.getHeightRectangle(j) - height.getLB()), this);
                    j++;
                }
            }
        }
    }

    protected boolean scalableTimeTableFilter(final List<Task> tasks, final List<IntVar> heights) throws ContradictionException {
        // From PropCumulativeGay2015
        boolean hasFiltered = false;
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (scalableTimeTableFilterEst(task, heights.get(i))) {
                hasFiltered = true;
                task.propagate(PropagatorEventType.FULL_PROPAGATION.getMask());
                shouldRecomputeTimeTable |= task.getLst() < task.getEct() && PropagatorResource.mustBePerformed(task, heights.get(i));
            }
            if (scalableTimeTableFilterLct(task, heights.get(i))) {
                hasFiltered = true;
                task.propagate(PropagatorEventType.FULL_PROPAGATION.getMask());
                shouldRecomputeTimeTable |= task.getLst() < task.getEct() && PropagatorResource.mustBePerformed(task, heights.get(i));
            }
        }
        return hasFiltered;
    }

    protected boolean scalableTimeTableFilterEst(final Task task, final IntVar height) throws ContradictionException {
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

    protected boolean scalableTimeTableFilterLct(final Task task, final IntVar height) throws ContradictionException {
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

    protected void computeTtAfter(final List<Task> tasks) {
        ttAfterMap.clear();
        set.clear();
        for (int i = 0; i < tasks.size(); ++i) {
            final int est = tasks.get(i).getEst();
            final int lct = tasks.get(i).getLct();
            set.add(est);
            set.add(lct);
        }
        list.clear();
        list.addAll(set);
        list.sort();
        int ttAfterTime = 0;
        int idx = profile.size() - 1;
        for (int k = 0; k < list.size(); ++k) {
            final int time = list.getQuick(list.size() - 1 - k);
            while (0 <= idx && time <= profile.getStartRectangle(idx)) {
                ttAfterTime += profile.getHeightRectangle(idx) * (profile.getEndRectangle(idx) - profile.getStartRectangle(idx));
                idx--;
            }
            final int value;
            if (0 <= idx && time < profile.getEndRectangle(idx)) {
                value = ttAfterTime + profile.getHeightRectangle(idx) * (profile.getEndRectangle(idx) - time);
            } else {
                value = ttAfterTime;
            }
            timeValues[list.size() - 1 - k].set(time);
            ttAfter[list.size() - 1 - k].set(value);
        }
        sizeTtAfter.set(list.size());
    }

    protected int getTtAfter(final int time) {
        return ttAfterMap.get(time);
    }

    protected void fillTtAfterMap() {
        ttAfterMap.clear();
        for (int i = 0; i < sizeTtAfter.get(); ++i) {
            ttAfterMap.put(timeValues[i].get(), ttAfter[i].get());
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

    protected void overloadChecking(final List<Task> tasks, final List<IntVar> heights) throws ContradictionException {
        // From PropCumulativeVilim2011
        if (hasRecomputedTimeTable) {
            computeTtAfter(tasks);
        }
        fillTtAfterMap();
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
                    if (capacity.getUB() * (lctB - tasks.get(a).getEst()) < eEF + getTtAfter(tasks.get(a).getEst()) - getTtAfter(lctB)) {
                        PropagatorResource.filterOptionalTask(tasks.get(b), heights.get(b), this);
                        break;
                    }
                }
            }
        }
    }

    protected void filter(final List<Task> tasks, final List<IntVar> heights) throws ContradictionException {
        scalableTimeTable(tasks, heights);
        overloadChecking(tasks, heights);
        updateHeights(tasks, heights);
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        final int v = idxVarInProp / 4;
        if (idxVarInProp >= 4 * tasks.length
            || tasks[v].hasCompulsoryPart() && PropagatorResource.mustBePerformed(tasks[v], heights[v])) {
            shouldRecomputeTimeTable = true;
        }
        forcePropagate(PropagatorEventType.CUSTOM_PROPAGATION);
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        hasRecomputedTimeTable = false;
        if (PropagatorEventType.isFullPropagation(evtmask)) {
            shouldRecomputeTimeTable = true;
        }
        computeMustBePerformedTasks();
        filter(performedAndOptionalTasks, tasksHeightsWithOptional);
    }
}
