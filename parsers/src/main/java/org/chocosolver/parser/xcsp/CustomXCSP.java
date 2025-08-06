/*
 * This file is part of choco-parsers, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.parser.xcsp;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDeg;
import org.chocosolver.solver.variables.IntVar;

public class CustomXCSP {
    public static void main(String[] args) throws Exception {
        XCSP xscp = new XCSP();
        String path = args[0];
        String[] arguments = {path, "-pa", "0", "-p", "1"};
        if(xscp.setUp(arguments)){
            xscp.createSolver();
            xscp.buildModel();
            Model model = xscp.getModel();
            Solver s = model.getSolver();
            IntVar[] vars = model.retrieveIntVars(true);
            DomOverWDeg cacd = new DomOverWDeg(vars, 0);
            s.setSearch(
                    Search.intVarSearch(cacd, new IntDomainMin(), vars)
            );
            s.clearRestarter();
            xscp.solve();
        }
    }
}
