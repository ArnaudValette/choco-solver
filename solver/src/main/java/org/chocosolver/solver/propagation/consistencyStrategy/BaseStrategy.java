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
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.util.objects.queues.RQ;
import org.chocosolver.util.objects.queues.ReinitialisableQueue;
import org.jgrapht.alg.util.Pair;

import java.util.ArrayList;

public abstract class BaseStrategy<T>  implements ISingletonConsistencyStrategy{
    protected RQ<T> Q;
    protected boolean changed=false;
    public ISingletonConsistencyStrategy ref;

    public ISingletonConsistencyStrategy ref(){
        return ref == null ? this : ref;
    }

    public void setRef(ISingletonConsistencyStrategy ref){
        this.ref = ref;
    }

    public abstract void setQ(RQ<T> Q);
}
