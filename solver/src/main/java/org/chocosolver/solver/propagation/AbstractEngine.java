/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.propagation;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;
import java.util.function.Consumer;

public abstract class AbstractEngine {
    public IPropagationEngine parent;

    public void setParent(IPropagationEngine parent) {
        this.parent=parent;
    }
    protected static class DynPropagators {

        private Propagator<?>[] elements;
        private int[] keys;
        private int size;

        DynPropagators() {
            elements = new Propagator[16];
            keys = new int[16];
            size = 0;
        }

        public void clear() {
            size = 0;
        }

        public void add(Propagator<?> e) {
            ensureCapacity();
            elements[size] = e;
            keys[size++] = Integer.MAX_VALUE;
        }

        private void ensureCapacity() {
            if (size >= elements.length - 1) {
                int nsize = ArrayUtils.newBoundedSize(elements.length, 8);
                elements = Arrays.copyOf(elements, nsize);
                keys = Arrays.copyOf(keys, nsize);
            }
        }

        void addOrUpdate(Propagator<?> e) {
            remove(e);
            add(e);
        }

        public void remove(Propagator<?> e) {
            int p = indexOf(e);
            if (p > -1) {
                removeAt(p);
            }
        }

        private void removeAt(int p) {
            if (p < size - 1) {
                System.arraycopy(elements, p + 1, elements, p, size - p);
                System.arraycopy(keys, p + 1, keys, p, size - p);
            }
            elements[--size] = null;
            keys[size] = 0;
        }

        private int indexOf(Propagator<?> e) {
            for (int i = 0; i < size; i++) {
                if (e.equals(elements[i])) {
                    return i;
                }
            }
            return -1;
        }

        void descending(int w, Consumer<Propagator<?>> cons) {
            int i = size - 1;
            while (i >= 0 && keys[i] >= w) {
                cons.accept(elements[i]);
                keys[i] = w;
                i--;
            }
        }
    }
}
