/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.util.objects.queues;

import java.util.function.Function;
import java.util.stream.Stream;

public interface RQ<T> {

    void setSupplier(Function<Void, Stream<T>> s);
    void lock();

    void reinit();
    boolean isEmpty();
    T pop();
    void add(T t);
    boolean contains(T t);

    int size();
}
