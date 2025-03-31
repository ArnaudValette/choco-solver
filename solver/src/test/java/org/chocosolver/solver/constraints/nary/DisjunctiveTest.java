/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Providers;
import org.chocosolver.solver.Settings;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Tests the various filtering algorithms of the cumulative constraint
 *
 * @author Thierry Petit, Jean-Guillaume Fages
 */
public class DisjunctiveTest {

    @Test(groups = "1s", timeOut = 60000, expectedExceptions = ContradictionException.class)
    public void testDur0TwoTasks() throws ContradictionException {
        Model m = new Model();

        Task t1 = new Task(
                m.intVar(0),
                m.intVar(9),
                m.intVar(9)
        );
        Task t2 = new Task(
                m.intVar(8),
                m.intVar(new int[]{0, 6}),
                m.intVar(8, 14)
        );
        m.disjunctive(new Task[]{t1, t2}).post();

        m.getSolver().propagate();
    }

    @Test(groups = "1s", timeOut = 60000, expectedExceptions = ContradictionException.class)
    public void testDur0ThreeTasks() throws ContradictionException {
        Model m = new Model();

        Task t1 = new Task(
                m.intVar(0),
                m.intVar(9),
                m.intVar(9)
        );
        Task t2 = new Task(
                m.intVar(8),
                m.intVar(new int[]{0, 6}),
                m.intVar(8, 14)
        );
        Task t3 = new Task(
                m.intVar(9),
                m.intVar(6),
                m.intVar(15)
        );
        m.disjunctive(new Task[]{t1, t2, t3}).post();

        m.getSolver().propagate();
    }
}
