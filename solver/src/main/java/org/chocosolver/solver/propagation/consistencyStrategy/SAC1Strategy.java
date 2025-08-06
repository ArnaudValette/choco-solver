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

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.consistencyStrategy.types.AbstractSAC1;

public class SAC1Strategy extends AbstractSAC1 {
    @Override
    public void onAfterInstantiation() throws ContradictionException {
        E.doPropagate();
        E.worldPopUntilNFlush(lastId);
        ref().baseState();
    }

    @Override
    public void passConsumer() throws ContradictionException {
        E.doPropagate();
    }
}
