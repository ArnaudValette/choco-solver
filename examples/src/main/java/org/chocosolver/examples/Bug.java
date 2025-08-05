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
import org.chocosolver.util.criteria.Criterion;
import org.jgrapht.alg.util.Pair;

import java.time.Duration;
import java.util.*;

public class Bug {
    public static void main(String[] args) {

        String[] inst = {
                // 0
                "/home/truite/lirmm/wip/instances/MiniCSP/Pentominoes-03-20_c24.xml.lzma",
                //1
                "/home/truite/lirmm/wip/instances/MiniCSP/AverageAvoiding-mini-20_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/AverageAvoiding-mini-30_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/AverageAvoiding-mini-35_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/AverageAvoiding-mini-40_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/AverageAvoiding-mini-45_c24.xml.lzma",
                // 6
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-hak-07-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-hak-08-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-hak-09-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-hak-10-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-hak-11-ogd2008_c24.xml.lzma",
                // 11
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-tab1-07-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-tab1-08-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-tab1-09-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-tab1-10-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-tab1-11-ogd2008_c24.xml.lzma",
        };
        try {
            XCSP x = new XCSP();
            String[] arg = {
                    inst[0],
                    "-pa", "0",
                    "-p", "1"};
            x.setUp(arg);
            x.createSolver();
            x.buildModel();
            Model model = x.getModel();
            Solver s =model.getSolver();


            IntVar[] vars = model.retrieveIntVars(true);
            DomOverWDeg cacd = new DomOverWDeg(vars,0);
            s.setSearch(
                    Search.intVarSearch(cacd, new IntDomainMin(), vars)
            );
            s.clearRestarter();
            s.showStatisticsDuringResolution(1000L);
            test(model);
        } catch (Exception e){

        }
    }

    public static void test(Model model) {

        try {
            //ISingletonConsistencyStrategy real = new NSAC1Strategy().setWillConsumePasses(true);
            //ISingletonConsistencyStrategy real = new NSAC1Strategy();
            //ISingletonConsistencyStrategy real = new SAC1Strategy().setWillConsumePasses(true);
            //ISingletonConsistencyStrategy real = new SAC1Strategy();
            //ISingletonConsistencyStrategy real = new SAC3Strategy();
            //ISingletonConsistencyStrategy real = new RNSQStrategy();
            //ISingletonConsistencyStrategy real = new RsNSQStrategy();
            //ISingletonConsistencyStrategy profiler = new EfficiencyObserver(real);
            //real.setRef(profiler);

            Solver s = model.getSolver();


            //model.getSolver().setEngine(new SingletonConsistencyEngine(model).setPropagationStrategy(profiler));
            model.getSolver().setEngine(new SingletonConsistencyEngine(model).setPropagationStrategy(new SAC1Strategy()));
            model.getSolver().reset();



        /*
        s.addStopCriterion(new Criterion() {
            @Override
            public boolean isMet() {
                return System.currentTimeMillis() - time > 10000;
            }
        });

         */
            //solveTest(model.getSolver());
            prune(model);
        }
        catch (Exception e){
            System.out.println(e);
        }

        //((EfficiencyObserver) profiler).printResults();
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

