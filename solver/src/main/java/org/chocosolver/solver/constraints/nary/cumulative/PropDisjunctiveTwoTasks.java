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

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;

import static org.chocosolver.solver.constraints.nary.cumulative.PropagatorResource.filterEst;
import static org.chocosolver.solver.constraints.nary.cumulative.PropagatorResource.filterLct;
import static org.chocosolver.solver.constraints.nary.cumulative.PropagatorResource.filterOptionalTask;
import static org.chocosolver.solver.constraints.nary.cumulative.PropagatorResource.intersect;
import static org.chocosolver.solver.constraints.nary.cumulative.PropagatorResource.mayBePerformed;
import static org.chocosolver.solver.constraints.nary.cumulative.PropagatorResource.mustBePerformed;

public class PropDisjunctiveTwoTasks extends Propagator<IntVar> {
    private static IntVar[] getVars(Task task1, IntVar use1, Task task2, IntVar use2) {
        if (use1 == null && use2 == null) {
            return new IntVar[]{task1.getStart(), task1.getDuration(), task1.getEnd(), task2.getStart(), task2.getDuration(), task2.getEnd()};
        } else if (use2 == null) {
            return new IntVar[]{task1.getStart(), task1.getDuration(), task1.getEnd(), task2.getStart(), task2.getDuration(), task2.getEnd(), use1};
        } else if (use1 == null) {
            return new IntVar[]{task1.getStart(), task1.getDuration(), task1.getEnd(), task2.getStart(), task2.getDuration(), task2.getEnd(), use2};
        } else {
            return new IntVar[]{
                    task1.getStart(),
                    task1.getDuration(),
                    task1.getEnd(),
                    task2.getStart(),
                    task2.getDuration(),
                    task2.getEnd(),
                    use1,
                    use2
            };
        }
    }

    private final Task task1;
    private final Task task2;
    private final IntVar use1;
    private final IntVar use2;

    public PropDisjunctiveTwoTasks(Task task1, Task task2) {
        this(task1, null, task2, null);
    }

    public PropDisjunctiveTwoTasks(Task task1, IntVar use1, Task task2, IntVar use2) {
        super(getVars(task1, use1, task2, use2), PropagatorPriority.BINARY, false);
        this.task1 = task1;
        this.task2 = task2;
        this.use1 = use1;
        this.use2 = use2;
    }

    @Override
    public int getPropagationConditions(int idx) {
        if (idx <= 6) { // start, duration, end variables
            return IntEventType.boundAndInst();
        } else { // use variables
            return IntEventType.lowerBoundAndInst();
        }
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (mayBePerformed(task1, use1) && mayBePerformed(task2, use2)) {
            boolean mustBePerformed1 = mustBePerformed(task1, use1);
            boolean mustBePerformed2 = mustBePerformed(task2, use2);
            if (PropagatorResource.intersect(task1, task2)) {
                if (mustBePerformed1) {
                    filterOptionalTask(task2, use2, this);
                } else if (mustBePerformed2) {
                    filterOptionalTask(task1, use1, this);
                }
            } else if (task1.getLst() < task2.getEct()) { // task1 before task2
                if (mustBePerformed1) {
                    filterEst(task2, use2, task1.getEct(), this);
                }
                if (mustBePerformed2) {
                    filterLct(task1, use1, task2.getLst(), this);
                }
            } else if (task2.getLst() < task1.getEct()) { // task2 is before task1
                if (mustBePerformed2) {
                    filterEst(task1, use1, task2.getEct(), this);
                }
                if (mustBePerformed1) {
                    filterLct(task2, use2, task1.getLst(), this);
                }
            }
        }
    }

    @Override
    public ESat isEntailed() {
        if (!mayBePerformed(task1, use1) || !mayBePerformed(task2, use2) || task1.getLct() <= task2.getEst() || task2.getLct() <= task1.getEst()) {
            return ESat.TRUE;
        } else if (mustBePerformed(task1, use1) && mustBePerformed(task2, use2) && intersect(task1, task2)) {
            return ESat.FALSE;
        } else {
            return ESat.UNDEFINED;
        }
    }
}
