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
import org.chocosolver.solver.propagation.PropagationEngine;
import org.chocosolver.solver.propagation.SingletonConsistencyEngine;
import org.chocosolver.solver.propagation.consistencyStrategy.*;
import org.chocosolver.solver.propagation.consistencyStrategy.benchmark.BenchmarkResults;
import org.chocosolver.solver.propagation.consistencyStrategy.benchmark.EfficiencyObserver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDeg;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
                    "-p", "1",
                    "-url", "http://localhost:3000/api/"};
            x.setUp(arg);
            x.createSolver();
            x.buildModel();
            Model model = x.getModel();
            Solver s =model.getSolver();

            IntVar[] vars = model.retrieveIntVars(true);

            DomOverWDeg cacd = new DomOverWDeg(vars, 0);
            s.setSearch(
                    Search.intVarSearch(cacd, new IntDomainMin(), vars)
            );

            s.clearRestarter();
            s.showStatisticsDuringResolution(1000L);
            SingletonConsistencyEngine engine = new SingletonConsistencyEngine(model);
            ISingletonConsistencyStrategy strategy = new SAC3Strategy();
            EfficiencyObserver obs = new EfficiencyObserver(strategy);

            strategy.setRef(obs);
            engine.setPropagationStrategy(obs);
            s.setEngine(engine);

            BenchmarkResults res = new BenchmarkResults(engine, obs);
            long time = System.currentTimeMillis();
            s.addStopCriterion(new Criterion() {
                @Override
                public boolean isMet() {
                    return System.currentTimeMillis() - time > 1000;
                }
            });

            s.solve();
            res.commit();
            String r = res.toJSON();
            System.out.println(r);

            try {
                /* make a post request to localhost:3000/api/  with r as data */
                URL url = new URL(x.url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                try(OutputStream os = conn.getOutputStream()){
                    os.write(r.getBytes(StandardCharsets.UTF_8));
                }
                int responseCode = conn.getResponseCode();
                System.out.println("Response code:" + responseCode);
                try(BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
                    String line;
                    while((line = in.readLine()) != null){
                       System.out.println(line);
                    }
                }
                conn.disconnect();
            } catch (Exception e){

            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    public static void test(Model model) {

        //model.getSolver().setEngine(new SAC1(model,null));

        IntVar[] vars = model.retrieveIntVars(true);
        int start = Arrays.stream(vars).reduce(0, (acc,next)-> acc + next.getDomainSize(), Integer::sum);

        PropagationEngine pe = model.getSolver().getEngine();

        int worldId= model.getEnvironment().getWorldIndex();
        model.getEnvironment().worldPush();

        pe.initialize();

        try {
            pe.propagate();
            pe.propagate();
            pe.propagate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int end = Arrays.stream(vars).reduce(0, (acc,next)-> acc + next.getDomainSize(), Integer::sum);
        int res1 = start-end;

        System.out.println("(nb of pruned values): " + res1);

        model.getEnvironment().worldPopUntil(worldId);
        //model.getSolver().solve();

    }

}
