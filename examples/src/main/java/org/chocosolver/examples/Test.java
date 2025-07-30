package org.chocosolver.examples;

import org.chocosolver.parser.xcsp.XCSP;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.propagation.PropagationEngine;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.*;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDeg;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.function.Function;

public class Test {
    public static void main(String[] args) {
        /*
        java -jar choco-parsers-5.0.0-beta.1-jar-with-dependencies.jar
        ~/lirmm/xcsp3/GraphColoring/GraphColoring-m1-fixed/GraphColoring-qwhdec-o70-h2940-1.xml.lzma
        -pa 0
        -p 1
        -sc 0
        -lvl INFO
        -f
        -restarts='[NONE,0,0,FALSE]'
        */
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
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-tab1-07-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-tab1-08-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-tab1-09-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-tab1-10-ogd2008_c24.xml.lzma",
                "/home/truite/lirmm/wip/instances/MiniCSP/WordSquare-tab1-11-ogd2008_c24.xml.lzma",
        };
        try {
            XCSP x = new XCSP();
            String[] arg = {
                    inst[1],
                    "-pa", "0",
            "-p", "1"};
            x.setUp(arg);
            x.createSolver();
            x.buildModel();
            Model model = x.getModel();
            Solver s =model.getSolver();

            IntVar[] vars = model.retrieveIntVars(true);
            DomOverWDeg<IntVar> cacd = new DomOverWDeg<>(vars, 0);
            s.setSearch(Search.inputOrderLBSearch(vars));
            s.clearRestarter();
            //x.getModel().getSolver().showSolutions();
            //x.getModel().getSolver().showStatisticsDuringResolution(1000L);
            //x.solve();
            Test.test(model);
           //x.getModel().getSolver().printStatistics();
        } catch (Exception e){

        }
    }

    public static void test(Model model) {
        NSAC1Strategy nsac1 = new NSAC1Strategy();
        SAC1Strategy sac1 = new SAC1Strategy();
        RNSQStrategy rnsq = new RNSQStrategy();
        SAC3Strategy sac3 = new SAC3Strategy();
        @SuppressWarnings("unchecked")
        Function<Model, Void>[] f = (Function<Model, Void>[]) new Function[]{
                (Function<Model, Void>) (m) -> {
                    m.getSolver().setEngine(new SingletonConsistencyEngine(m).setPropagationStrategy(sac3));
                    return null;
                },
                (Function<Model, Void>) (m) -> {
                    m.getSolver().setEngine(new SingletonConsistencyEngine(m).setPropagationStrategy(sac1));
                    return null;
                },
                (Function<Model, Void>) (m) -> {
                    m.getSolver().setEngine(new SingletonConsistencyEngine(m).setPropagationStrategy(nsac1));
                    return null;
                },
                (Function<Model, Void>) (m) -> {
                    m.getSolver().setEngine(new SingletonConsistencyEngine(m).setPropagationStrategy(rnsq));
                    return null;
                },
                (Function<Model, Void>) (m) -> {
                    m.getSolver().setEngine(new TestSAC(m, null));
                    return null;
                },
        };

            IntVar[] vars = model.retrieveIntVars(true);
            int start = Arrays.stream(vars).reduce(0, (acc,next)-> acc + next.getDomainSize(), Integer::sum);
            f[4].apply(model);
            model.getSolver().showDecisions();
            PropagationEngine pe = model.getSolver().getEngine();
            int worldId= model.getEnvironment().getWorldIndex();
            model.getEnvironment().worldPush();
            pe.initialize();
            try {
                pe.propagate();
                pe.propagate();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //vars = model.retrieveIntVars(true);
            int end = Arrays.stream(vars).reduce(0, (acc,next)-> acc + next.getDomainSize(), Integer::sum);
            int res1 = start-end;

            System.out.println("(nb of pruned values): " + res1);

            model.getEnvironment().worldPopUntil(worldId);


            //model.getSolver().solve();

        //}
    }

}
