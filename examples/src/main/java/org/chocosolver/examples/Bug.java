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
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDeg;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.benchmark.BenchResult;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Bug {
    public static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public static BenchResult benchmark(String alg, String path){
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
                //ISingletonConsistencyStrategy real = new NSAC1Strategy().setWillConsumePasses(true);
                //ISingletonConsistencyStrategy real = new NSAC1Strategy();
                //ISingletonConsistencyStrategy real = new SAC1Strategy().setWillConsumePasses(true);
                //ISingletonConsistencyStrategy real = new SAC1Strategy();
                //ISingletonConsistencyStrategy real = new SAC3Strategy();
                //ISingletonConsistencyStrategy real = new RNSQStrategy();
                ISingletonConsistencyStrategy real = new RsNSQStrategy();
                ISingletonConsistencyStrategy profiler = new EfficiencyObserver(real);
                real.setRef(profiler);
                model.getSolver().setEngine(new SingletonConsistencyEngine(model).setPropagationStrategy(profiler));
                model.getSolver().reset();
                solveTest(model.getSolver());
                ((EfficiencyObserver) profiler).printResults();
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
        HashMap<String, BenchResult> results = new HashMap<>();
        File dir = new File(System.getProperty("user.home") + "/MiniCSP");
        File[] files = dir.listFiles();
        List<String> paths = Arrays.stream(files).filter(File::isFile).map(File::getAbsolutePath).sorted().collect(Collectors.toList());

        int algo_count = 7;
        int max_batch_size = 11;
        int consumed = 0;
        int size = paths.size();

        while (consumed < size) {
            int remaining = size-consumed;
            int next_batch_size = Math.min(max_batch_size, remaining);

            if (Runtime.getRuntime().availableProcessors() >= (next_batch_size * algo_count)) {
                ExecutorService executor = Executors.newFixedThreadPool(next_batch_size * algo_count);
                for(int i = 0; i < next_batch_size; i++){
                    String instance = paths.get(consumed + i);
                    String[] algs = {"AC", "SAC1", "pSAC1", "SAC3", "RNSQ","RsNSQ", "NSAC"};
                    for(String algo : algs){
                        final String inst = instance;
                        final String alg = algo;
                        executor.submit(()->{
                            BenchResult result = benchmark(alg, inst);
                            synchronized (results){
                                results.put(alg + ":" + inst, result);
                            }
                        });
                    }
                }
                executor.shutdown();
                try {
                    executor.awaitTermination(1, TimeUnit.HOURS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                consumed+= next_batch_size;
            }
            sleep(60000L);
        }
    }

    public static void test(Model model) {

        try {
            //ISingletonConsistencyStrategy real = new NSAC1Strategy().setWillConsumePasses(true);
            //ISingletonConsistencyStrategy real = new NSAC1Strategy();
            //ISingletonConsistencyStrategy real = new SAC1Strategy().setWillConsumePasses(true);
            //ISingletonConsistencyStrategy real = new SAC1Strategy();
            //ISingletonConsistencyStrategy real = new SAC3Strategy();
            ISingletonConsistencyStrategy real = new RNSQStrategy();
            //ISingletonConsistencyStrategy real = new RsNSQStrategy();
            ISingletonConsistencyStrategy profiler = new EfficiencyObserver(real);
            real.setRef(profiler);

            Solver s = model.getSolver();


            model.getSolver().setEngine(new SingletonConsistencyEngine(model).setPropagationStrategy(profiler));
            //model.getSolver().setEngine(new SACEngine(model,null));
            model.getSolver().reset();



        /*
        s.addStopCriterion(new Criterion() {
            @Override
            public boolean isMet() {
                return System.currentTimeMillis() - time > 10000;
            }
        });

         */
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

