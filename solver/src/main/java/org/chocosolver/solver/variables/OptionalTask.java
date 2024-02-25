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

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;

/**
 * Container representing an optional task:
 * It ensures that: start + duration = end, if and only if the task is performed
 *
 * @author Arthur Godet
 * @since 25/11/2023
 */
public class OptionalTask extends Task {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private final BoolVar performed;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Container representing an optional task:
     * It ensures that: start + duration = end, if and only if the task is performed, end being an offset view of start + duration.
     *
     * @param model the Model of the variables
     * @param est earliest starting time
     * @param lst latest starting time
     * @param d duration
     * @param ect earliest completion time
     * @param lct latest completion time
     */
    public OptionalTask(Model model, int est, int lst, int d, int ect, int lct) {
        this(model, est, lst, d, ect, lct, model.boolVar());
    }

    /**
     * Container representing an optional task:
     * It ensures that: start + duration = end, if and only if the task is performed, end being an offset view of start + duration.
     *
     * @param model the Model of the variables
     * @param est earliest starting time
     * @param lst latest starting time
     * @param d duration
     * @param ect earliest completion time
     * @param lct latest completion time
     * @param performed performed variable
     */
    public OptionalTask(Model model, int est, int lst, int d, int ect, int lct, BoolVar performed) {
        super(model, est, lst, d, ect, lct);
        this.performed = performed;
    }

    /**
     * Container representing an optional task:
     * It ensures that: start + duration = end, if and only if the task is performed, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     */
    public OptionalTask(IntVar s, int d) {
        this(s, d, s.getModel().boolVar());
    }

    /**
     * Container representing an optional task:
     * It ensures that: start + duration = end, if and only if the task is performed, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     * @param performed performed variable
     */
    public OptionalTask(IntVar s, int d, BoolVar performed) {
        super(s, d);
        this.performed = performed;
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, if and only if the task is performed.
     *
     * @param s start variable
     * @param d duration variable
     */
    public OptionalTask(IntVar s, IntVar d) {
        this(s, d, s.getModel().boolVar());
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, if and only if the task is performed.
     *
     * @param s start variable
     * @param d duration variable
     * @param performed performed variable
     */
    public OptionalTask(IntVar s, IntVar d, BoolVar performed) {
        super(s, d);
        this.performed = performed;
    }

    /**
     * Container representing an optional task:
     * It ensures that: start + duration = end, if and only if the task is performed, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     * @param e end variable
     */
    public OptionalTask(IntVar s, int d, IntVar e) {
        this(s, d, e, s.getModel().boolVar());
    }

    /**
     * Container representing an optional task:
     * It ensures that: start + duration = end, if and only if the task is performed, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     * @param e end variable
     * @param performed performed variable
     */
    public OptionalTask(IntVar s, int d, IntVar e, BoolVar performed) {
        super(s, d, e);
        this.performed = performed;
    }

    /**
     * Container representing an optional task:
     * It ensures that: start + duration = end, if and only if the task is performed
     *
     * @param s start variable
     * @param d duration variable
     * @param e end variable
     */
    public OptionalTask(IntVar s, IntVar d, IntVar e) {
        this(s, d, e, s.getModel().boolVar());
    }

    /**
     * Container representing an optional task:
     * It ensures that: start + duration = end, if and only if the task is performed
     *
     * @param s start variable
     * @param d duration variable
     * @param e end variable
     * @param performed performed variable
     */
    public OptionalTask(IntVar s, IntVar d, IntVar e, BoolVar performed) {
        super(s, d, e);
        this.performed = performed;
    }

    //***********************************************************************************
    // ACCESSORS
    //***********************************************************************************

    @Override
    public boolean mayBePerformed() {
        return performed.getUB() == 1;
    }

    @Override
    public boolean mustBePerformed() {
        return performed.getLB() == 1;
    }

    public BoolVar getPerformed() {
        return this.performed;
    }

    @Override
    public int getEst() {
        if (mayBePerformed()) {
            return start.getLB();
        } else {
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public int getLst() {
        if (mayBePerformed()) {
            return start.getUB();
        } else {
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public int getEct() {
        if (mayBePerformed()) {
            return end.getLB();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getLct() {
        if (mayBePerformed()) {
            return end.getUB();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getMinDuration() {
        if (mayBePerformed()) {
            return duration.getLB();
        } else {
            return 0;
        }
    }

    @Override
    public int getMaxDuration() {
        if (mayBePerformed()) {
            return duration.getUB();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public boolean forceToBePerformed(ICause cause) throws ContradictionException {
        return performed.updateLowerBound(1, cause);
    }

    @Override
    public boolean forceToBeOptional(ICause cause) throws ContradictionException {
        return performed.updateUpperBound(0, cause);
    }

    @Override
    public boolean updateEst(int est, ICause cause) throws ContradictionException {
        if (mayBePerformed()) {
            try {
                return start.updateLowerBound(est, cause);
            } catch (ContradictionException ex) {
                performed.updateUpperBound(0, cause);
            }
        }
        return false;
    }

    @Override
    public boolean updateLst(int lst, ICause cause) throws ContradictionException {
        if (mayBePerformed()) {
            try {
                return start.updateUpperBound(lst, cause);
            } catch (ContradictionException ex) {
                performed.updateUpperBound(0, cause);
            }
        }
        return false;
    }

    @Override
    public boolean updateEct(int ect, ICause cause) throws ContradictionException {
        if (mayBePerformed()) {
            try {
                return end.updateLowerBound(ect, cause);
            } catch (ContradictionException ex) {
                performed.updateUpperBound(0, cause);
            }
        }
        return false;
    }

    @Override
    public boolean instantiateStartAt(int t, ICause cause) throws ContradictionException {
        if (mayBePerformed()) {
            try {
                return start.instantiateTo(t, cause);
            } catch (ContradictionException ex) {
                performed.updateUpperBound(0, cause);
            }
        }
        return false;
    }

    @Override
    public boolean instantiateEndAt(int t, ICause cause) throws ContradictionException {
        if (mayBePerformed()) {
            try {
                return end.instantiateTo(t, cause);
            } catch (ContradictionException ex) {
                performed.updateUpperBound(0, cause);
            }
        }
        return false;
    }

    @Override
    public boolean updateLct(int lct, ICause cause) throws ContradictionException {
        if (mayBePerformed()) {
            try {
                return end.updateUpperBound(lct, cause);
            } catch (ContradictionException ex) {
                performed.updateUpperBound(0, cause);
            }
        }
        return false;
    }

    @Override
    public boolean updateDuration(int minDuration, int maxDuration, ICause cause) throws ContradictionException {
        if (mayBePerformed()) {
            try {
                return duration.updateBounds(minDuration, maxDuration, cause);
            } catch (ContradictionException ex) {
                performed.updateUpperBound(0, cause);
            }
        }
        return false;
    }

    @Override
    public Task getMirror() {
        if (mirror == null) {
            mirror = new OptionalTask(end.neg().intVar(), duration, start.neg().intVar(), performed);
        }
        return mirror;
    }

    @Override
    public String toString() {
        return "OptionalTask[" +
                "start=" + start +
                ", duration=" + duration +
                ", end=" + end +
                ", performed=" + performed +
                ']';
    }
}
