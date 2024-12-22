/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2024, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.variables;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.learn.ExplanationForSignedClause;
import org.chocosolver.solver.variables.view.integer.IntAffineView;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.iterable.IntIterableRangeSet;

import java.util.function.Consumer;

import static org.chocosolver.util.objects.setDataStructures.iterable.IntIterableSetUtils.unionOf;

/**
 * Container representing a task:
 * It ensures that: start + duration = end
 *
 * @author Jean-Guillaume Fages
 * @since 04/02/2013
 */
public class Task extends Propagator<IntVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    protected final IntVar start;
    protected final IntVar duration;
    protected final IntVar end;

    protected Task mirror = null;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param model the Model of the variables
     * @param est earliest starting time
     * @param lst latest starting time
     * @param d duration
     * @param ect earliest completion time
     * @param lct latest completion time
     */
    public Task(Model model, int est, int lst, int d, int ect, int lct) {
        this(buildVars(model, est, lst, d, ect, lct));
    }

    private static IntVar[] buildVars(Model model, int est, int lst, int d, int ect, int lct) {
        IntVar start = model.intVar(Math.max(est, ect - d), Math.min(lst, lct - d));
        IntVar duration = model.intVar(d);
        IntVar end = start.getModel().offset(start, d);
        return new IntVar[]{start, duration, end};
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     */
    public Task(IntVar s, int d) {
        this(new IntVar[]{s, s.getModel().intVar(d), s.getModel().offset(s, d)});
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     */
    public Task(IntVar s, IntVar d) {
        this(new IntVar[]{
                s,
                d,
                d.isInstantiated() ? s.getModel().offset(s, d.getValue())
                                   : s.getModel().intVar(s.getLB() + d.getLB(), s.getUB() + d.getUB())
        });
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     * @param e end variable
     */
    public Task(IntVar s, int d, IntVar e) {
        this(s, s.getModel().intVar(d), e);
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end
     *
     * @param s start variable
     * @param d duration variable
     * @param e end variable
     */
    public Task(IntVar s, IntVar d, IntVar e) {
        super(new IntVar[]{s, d, e}, PropagatorPriority.TERNARY, false, false);
        start = s;
        duration = d;
        end = e;
        if (shouldPassivate(s, d, e)) {
            setActive();
            setPassive();
        } else {
            this.getModel().post(new Constraint("Task relation", this));
        }
    }

    private Task(IntVar[] vars) {
        this(vars[0], vars[1], vars[2]);
    }

    public static boolean isOffsetView(IntVar s, int d, IntVar e) {
        if (e instanceof IntAffineView) {
            IntAffineView<?> intOffsetView = (IntAffineView<?>) e;
            if (intOffsetView.p) {
                return intOffsetView.equals(s, 1, d);
            } else {
                intOffsetView = (IntAffineView<?>) s;
                return intOffsetView.equals(e.neg().intVar(), -1, -d);
            }
        }
        return false;
    }

    public static boolean shouldPassivate(final IntVar s, final IntVar d, final IntVar e) {
        return d.isInstantiated() && isOffsetView(s, d.getValue(), e);
    }

    //***********************************************************************************
    // ACCESSORS
    //***********************************************************************************

    public IntVar getStart() {
        return start;
    }

    public IntVar getDuration() {
        return duration;
    }

    public IntVar getEnd() {
        return end;
    }

    public int getEst() {
        return start.getLB();
    }

    public int getLst() {
        return start.getUB();
    }

    public int getEct() {
        return end.getLB();
    }

    public int getLct() {
        return end.getUB();
    }

    public int getMinDuration() {
        return duration.getLB();
    }

    public int getMaxDuration() {
        return duration.getUB();
    }

    public boolean isStartInstantiated() {
        return start.isInstantiated();
    }

    public boolean isEndInstantiated() {
        return end.isInstantiated();
    }

    public boolean updateEst(int est, ICause cause) throws ContradictionException {
        return start.updateLowerBound(est, cause);
    }

    public boolean updateLst(int lst, ICause cause) throws ContradictionException {
        return start.updateUpperBound(lst, cause);
    }

    public boolean updateEct(int ect, ICause cause) throws ContradictionException {
        return end.updateLowerBound(ect, cause);
    }

    public boolean updateLct(int lct, ICause cause) throws ContradictionException {
        return end.updateUpperBound(lct, cause);
    }

    public boolean updateMinDuration(int minDuration, ICause cause) throws ContradictionException {
        return duration.updateLowerBound(minDuration, cause);
    }

    public boolean updateMaxDuration(int maxDuration, ICause cause) throws ContradictionException {
        return duration.updateUpperBound(maxDuration, cause);
    }

    public boolean updateDuration(int minDuration, int maxDuration, ICause cause) throws ContradictionException {
        return duration.updateBounds(minDuration, maxDuration, cause);
    }

    public boolean instantiateStartAt(int t, ICause cause) throws ContradictionException {
        return start.instantiateTo(t, cause);
    }

    public boolean instantiateEndAt(int t, ICause cause) throws ContradictionException {
        return end.instantiateTo(t, cause);
    }

    public boolean mayBePerformed() {
        return true;
    }

    public boolean mustBePerformed() {
        return true;
    }

    public boolean forceToBePerformed(ICause cause) throws ContradictionException {
        return false;
    }

    public boolean forceToBeOptional(ICause cause) throws ContradictionException {
        ContradictionException ex = new ContradictionException();
        ex.set(cause, null, "forcing Task to be optional");
        throw ex;
    }

    public Task getMirror() {
        if (mirror == null) {
            mirror = new Task(end.neg().intVar(), duration, start.neg().intVar());
        }
        return mirror;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        boolean hasFiltered;
        do {
            hasFiltered = false;
            if (mayBePerformed()) {
                hasFiltered = updateEst(end.getLB() - duration.getUB(), this);
                hasFiltered |= updateLst(end.getUB() - duration.getLB(), this);

                hasFiltered |= updateEct(start.getLB() + duration.getLB(), this);
                hasFiltered |= updateLct(start.getUB() + duration.getUB(), this);

                hasFiltered |= updateDuration(end.getLB() - start.getUB(), end.getUB() - start.getLB(), this);
            }
        } while (hasFiltered);
    }

    @Override
    public ESat isEntailed() {
        if (start.isInstantiated() && duration.isInstantiated() && end.isInstantiated()) {
            return ESat.eval(start.getValue() + duration.getValue() == end.getValue());
        } else {
            return ESat.UNDEFINED;
        }
    }

    private static void doExplain(IntVar S, IntVar D, IntVar E,
                                  int p,
                                  ExplanationForSignedClause explanation) {
        IntVar pivot = explanation.readVar(p);
        IntIterableRangeSet dom;
        dom = explanation.complement(S);
        if (S == pivot) {
            unionOf(dom, explanation.readDom(p));
            S.intersectLit(dom, explanation);
        } else {
            S.unionLit(dom, explanation);
        }
        dom = explanation.complement(D);
        if (D == pivot) {
            unionOf(dom, explanation.readDom(p));
            D.intersectLit(dom, explanation);
        } else {
            D.unionLit(dom, explanation);
        }
        dom = explanation.complement(E);
        if (E == pivot) {
            unionOf(dom, explanation.readDom(p));
            E.intersectLit(dom, explanation);
        } else {
            E.unionLit(dom, explanation);
        }
    }

    @Override
    public String toString() {
        return "Task[" +
                "start=" + start +
                ", duration=" + duration +
                ", end=" + end +
                ']';
    }

    @Override
    public void explain(int p, ExplanationForSignedClause explanation) {
        doExplain(start, duration, end, p, explanation);
    }

    @Override
    public void forEachIntVar(Consumer<IntVar> action) {
        action.accept(start);
        action.accept(duration);
        action.accept(end);
    }
}
