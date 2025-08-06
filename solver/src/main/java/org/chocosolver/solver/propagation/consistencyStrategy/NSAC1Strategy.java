/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.propagation.consistencyStrategy;

import java.util.BitSet;

public class NSAC1Strategy extends SAC1Strategy{

    @Override
    public void onBeforeInstantiation(){
        ref().baseState();
        E.setDirectPropsScheduling(nsac);
        E.setDoFilterScheduling(true);
    }
}
