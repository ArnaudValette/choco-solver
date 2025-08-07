/*
 * This file is part of choco-parsers, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.parser.xcsp;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
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

public class CustomXCSP {
    public static void main(String[] args) throws Exception {
        XCSP xscp = new XCSP();
        if(xscp.setUp(args)){
            xscp.createSolver();
            xscp.buildModel();
            Model model = xscp.getModel();
            Solver s = model.getSolver();
            IntVar[] vars = model.retrieveIntVars(true);
            DomOverWDeg cacd = new DomOverWDeg(vars, 0);
            s.setSearch(
                    Search.intVarSearch(cacd, new IntDomainMin(), vars)
            );
            s.clearRestarter();
            SingletonConsistencyEngine engine = new SingletonConsistencyEngine(model);
            AbstractSingletonStrategy strategy;
            switch(xscp.sc){
                case NONE:
                    strategy = new NoStrategy().setDoPropagate(false);
                    System.out.println("Using brute-force");
                    break;
                case SAC3:
                    strategy = new SAC3Strategy();
                    System.out.println("Using SAC3");
                    break;
                case SAC1:
                    strategy = new SAC1Strategy();
                    System.out.println("Using SAC1");
                    break;
                case RsNSQ:
                    strategy = new RsNSQStrategy();
                    System.out.println("Using RsNSQ");
                    break;
                case RNSQ:
                    strategy = new RNSQStrategy();
                    System.out.println("Using RNSQ");
                    break;
                case NSAC:
                    strategy = new NSAC1Strategy();
                    System.out.println("Using NSAC1");
                    break;
                case AC:
                    strategy = new NoStrategy().setDoPropagate(true);
                    System.out.println("Using AC");
                    break;
                default:
                    strategy = new NoStrategy().setDoPropagate(true);
                    System.out.println("Using AC");
            }
            if(xscp.passes !=0){
                engine.setPasses(xscp.passes);
                strategy.setWillConsumePasses(true);
                System.out.println("Restrict to " + xscp.passes + " passes");
            }

            EfficiencyObserver obs = new EfficiencyObserver(strategy);
            BenchmarkResults res = new BenchmarkResults(engine, obs);

            if(xscp.monitor){
                strategy.setRef(obs);
                engine.setPropagationStrategy(obs);
            }
            else{
                engine.setPropagationStrategy(strategy);
            }
            s.setEngine(engine);
            if(xscp.timeout != 0){
                long time = System.currentTimeMillis();
                s.addStopCriterion(new Criterion() {
                    @Override
                    public boolean isMet() {
                        return System.currentTimeMillis() - time > xscp.timeout;
                    }
                });
            }

            xscp.solve();

            if(xscp.monitor){
                res.commit();
                handleResults(res, xscp.url);
            }
        }
    }
    private static void handleResults(BenchmarkResults r, String url){
        if(url != null){
            postToUrl(r,url);
        }
        else{
            System.out.println(r.toJSON());
        }

    }

    private static void postToUrl(BenchmarkResults r, String uri){
        URL url;
        try {
            String jsonData = r.toJSON();
            url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStream os = conn.getOutputStream();
            os.write(jsonData.getBytes(StandardCharsets.UTF_8));
            int responseCode = conn.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while((line = in.readLine()) != null){
                    System.out.println(line);
                }
            }
            else{
                System.err.println("Request failed (response status != 200)");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while((line = in.readLine()) != null){
                    System.out.println(line);
                }
            }
            conn.disconnect();
        } catch (Exception e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

    }
}
