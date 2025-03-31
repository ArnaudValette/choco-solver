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

import static org.chocosolver.solver.search.strategy.Search.lastConflict;

/**
 * Tests the various filtering algorithms of the cumulative constraint
 *
 * @author Thierry Petit, Jean-Guillaume Fages, Arthur GODET <arth.godet@gmail.com>
 */
public class CumulativeTest {
    @Test(groups = "1s", timeOut = 60000)
    public void testDur0() {
        Model m = new Model();

        Task t1 = new Task(
                m.intVar(9),
                m.intVar(6),
                m.intVar(15)
        );
        Task t2 = new Task(
                m.intVar(8),
                m.intVar(new int[]{0, 6}),
                m.intVar(8, 14)
        );

        m.cumulative(new Task[]{t1, t2}, new IntVar[]{m.intVar(1), m.intVar(1)}, m.intVar(1)).post();

        Solver s = m.getSolver();

        try {
            s.propagate();
        } catch (ContradictionException e) {
            e.printStackTrace();
            Assert.fail();
        }
        Assert.assertTrue(t2.getDuration().isInstantiatedTo(0));
    }

    @Test(groups = "10s", timeOut = 60000)
    public void testADelsol1() {
        int[] height = new int[]{0, 1, 3, 5, 1, 4, 4, 3, 4, 3, 0};
        int capaMax = 10;
        int[] duration = new int[11];
        Arrays.fill(duration, 1);
        // dÃ©claration du modÃ¨le
        Model model = new Model("test");
        // Ajout des starting times
        IntVar[] start = model.intVarArray("start", 11, 0, 3);
        model.cumulative(start, duration, height, capaMax).post();

        Solver solver = model.getSolver();
        while (solver.solve()) {
            for (int time = 0; time < 4; ++time) {
                int max_height = 0;
                for (int i = 0; i < 11; ++i) {
                    if (start[i].getValue() == time) max_height += height[i];
                }
                Assert.assertTrue(max_height <= capaMax);
            }
        }
    }

    @Test(groups = "1s", dataProvider = "trueOrFalse", dataProviderClass = Providers.class)
    public void testGCCAT(boolean lcg) {
        Model model = new Model(Settings.dev().setLCG(lcg));
        int[][] s = new int[][]{{1, 5}, {2, 7}, {3, 6}, {1, 8}};
        int[][] d = new int[][]{{4, 4}, {6, 6}, {3, 6}, {2, 3}};
        int[][] e = new int[][]{{1, 9}, {1, 9}, {1, 9}, {1, 9}};
        int[][] h = new int[][]{{2, 6}, {3, 3}, {1, 2}, {3, 4}};

        Task[] tasks = IntStream.range(0, 4)
                .mapToObj(i -> new Task(
                        model.intVar("s" + i, s[i][0], s[i][1]),
                        model.intVar("d" + i, d[i][0], d[i][1]),
                        model.intVar("e" + i, e[i][0], e[i][1])))
                .toArray(Task[]::new);

        IntVar[] height = IntStream.range(0, 4)
                .mapToObj(i -> model.intVar("h" + i, h[i][0], h[i][1]))
                .toArray(IntVar[]::new);

        model.cumulative(tasks, height, model.intVar(5)).post();
        Solver solver = model.getSolver();
        solver.findAllSolutions();

        Assert.assertEquals(solver.getSolutionCount(), 8);
    }
}
