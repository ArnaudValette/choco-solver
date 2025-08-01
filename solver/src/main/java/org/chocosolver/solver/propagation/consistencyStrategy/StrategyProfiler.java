package org.chocosolver.solver.propagation.consistencyStrategy;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Map;

public class StrategyProfiler {
    @SuppressWarnings("unchecked")
    public static <T> T create(T target, Map<String, ArrayDeque<Long>> timings){
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                new Class<?>[] {ISingletonConsistencyStrategy.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        long start = System.nanoTime();
                        try{
                            return method.invoke(target, args);
                        } catch(InvocationTargetException e){
                            throw e.getCause();
                        }
                        finally {
                            long duration = System.nanoTime() - start;
                            timings.computeIfAbsent(method.getName(), k -> new ArrayDeque<>()).addLast(duration);
                        }
                    }
                }
        );
    }
}
