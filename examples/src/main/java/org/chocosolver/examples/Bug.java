/*
 * This file is part of examples, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.examples;


import org.chocosolver.parser.xcsp.XCSP;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.propagation.*;
import org.chocosolver.solver.propagation.consistencyStrategy.*;
import org.chocosolver.solver.propagation.consistencyStrategy.benchmark.BenchmarkResults;
import org.chocosolver.solver.propagation.consistencyStrategy.benchmark.EfficiencyObserver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDeg;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.benchmark.BenchResult;

import java.util.concurrent.*;


public class Bug {
    public static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public static BenchResult benchmark(String path){
        try {
            XCSP x = new XCSP();
            String[] arg = {
                path,
                "-pa", "0",
                "-p", "1"};
            x.setUp(arg);
            x.createSolver();
            x.buildModel();
            Model model = x.getModel();
            Solver s = model.getSolver();


            IntVar[] vars = model.retrieveIntVars(true);
            DomOverWDeg cacd = new DomOverWDeg(vars, 0);
            s.setSearch(
                    Search.intVarSearch(cacd, new IntDomainMin(), vars)
            );

            s.clearRestarter();
            s.showStatisticsDuringResolution(1000L);
            try {
                model.getSolver().reset();
                ISingletonConsistencyStrategy real = new NoStrategy().setDoPropagate(true);
                //ISingletonConsistencyStrategy real = new RsNSQStrategy().setWillConsumePasses(true);
                //ISingletonConsistencyStrategy real = new NSAC1Strategy().setWillConsumePasses(false);
                ;
                long time = System.currentTimeMillis();


                EfficiencyObserver profiler = new EfficiencyObserver(real);
                real.setRef(profiler);
                SingletonConsistencyEngine engine = new SingletonConsistencyEngine(model).setPropagationStrategy(profiler).setPasses(1);

                BenchmarkResults res = new BenchmarkResults(engine, profiler);
                model.getSolver().setEngine(engine);

                ExecutorService ex = Executors.newSingleThreadExecutor();
                Future<?> f = ex.submit(()-> {
                            model.getSolver().getEngine().initialize();
                            model.getSolver().solve();
                        });
                boolean timedOut = false;
                try{
                    f.get(10, TimeUnit.SECONDS);
                }
                catch(TimeoutException e){
                    System.out.println("TimedOUT");
                    timedOut = true;
                    f.cancel(true);
                }
                catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e){
                    throw new RuntimeException(e.getCause());
                }
                finally {
                    if(timedOut){
                        model.getSolver().addStopCriterion(()->true);
                    }
                    ex.shutdownNow();
                    ex.awaitTermination(5, TimeUnit.SECONDS);

                }

                //prune(model);
                res.commit(timedOut);
                System.out.println(res.toJSON());
                //model.getSolver().printStatistics();

            }
            catch (Exception e){
                System.out.println(e);
                e.printStackTrace();
            }

        } catch (Exception e) {

        }
        return new BenchResult();
    }

    public static void main(String[] args) {
        //benchmark("/home/truite/MiniCSP/MisteryShopper-mini-8-12-1-6-0_c24.xml.lzma");
        //benchmark("/home/truite/MiniCSP/MisteryShopper-mini-8-12-3-6-0_c24.xml.lzma");
        //benchmark("/home/truite/MiniCSP/AverageAvoiding-mini-45_c24.xml.lzma");
        benchmark("/home/truite/MiniCSP/AverageAvoiding-mini-20_c24.xml.lzma");
    }

    public static void test(Model model) {

        try {
            ISingletonConsistencyStrategy real = new RNSQStrategy();
            ISingletonConsistencyStrategy profiler = new EfficiencyObserver(real);
            real.setRef(profiler);

            Solver s = model.getSolver();


            model.getSolver().setEngine(new SingletonConsistencyEngine(model).setPropagationStrategy(profiler));
            model.getSolver().reset();



            solveTest(model.getSolver());
            //prune(model);
            ((EfficiencyObserver) profiler).printResults();
        }
        catch (Exception e){
            System.out.println(e);
            e.printStackTrace();
        }

    }

        public static void solveTest(Solver s){
        try {
            s.solve();
            s.printStatistics();
        } catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    public static void prune(Model model) {
        PropagationEngine pe = model.getSolver().getEngine();


        int start = 0;
        for (IntVar v : model.retrieveIntVars(true)) {
            for (int value = v.getLB(); value <= v.getUB(); value = v.nextValue(value)) {
                start++;
            }
        }

        try {
            pe.initialize();
            pe.propagate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        int end = 0;
        for (IntVar v : model.retrieveIntVars(true)) {
            for (int value = v.getLB(); value <= v.getUB(); value = v.nextValue(value)) {
                end++;
            }
        }
        int res1 = start - end;

        System.out.println("(nb of pruned values): " + res1);
        long time = System.currentTimeMillis();
    }

}

