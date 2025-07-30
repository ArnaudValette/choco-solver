package org.chocosolver.examples;


import org.chocosolver.parser.xcsp.XCSP;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.propagation.PropagationEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.*;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import java.util.Arrays;

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
            s.setSearch(Search.inputOrderLBSearch(vars));

            s.clearRestarter();
            Test.test(model);
        } catch (Exception e){

        }
    }

    public static void test(Model model) {

        model.getSolver().setEngine(new TestSAC(model,null));

        IntVar[] vars = model.retrieveIntVars(true);
        int start = Arrays.stream(vars).reduce(0, (acc, next)-> acc + next.getDomainSize(), Integer::sum);


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

        int end = Arrays.stream(vars).reduce(0, (acc,next)-> acc + next.getDomainSize(), Integer::sum);
        int res1 = start-end;

        System.out.println("(nb of pruned values): " + res1);

        model.getEnvironment().worldPopUntil(worldId);
    }
}

