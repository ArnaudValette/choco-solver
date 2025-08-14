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

import org.chocosolver.parser.RegParser;
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;

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
                case onepSAC1:
                    strategy = new SAC1Strategy().setWillConsumePasses(true);
                    engine.setPasses(1);
                    break;
                case RsNS1pQ:
                    strategy = new RsNSQStrategy().setWillConsumePasses(true);
                    engine.setPasses(1);
                    break;
                case RNS1pQ:
                    strategy = new RNSQStrategy().setWillConsumePasses(true);
                    engine.setPasses(1);
                    break;
                case N1pSAC:
                    strategy = new NSAC1Strategy().setWillConsumePasses(true);
                    engine.setPasses(1);
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
                // propagation monitoring is memory-heavy
                obs.doMonitorPropagations=false;
                strategy.setRef(obs);
                engine.setPropagationStrategy(obs);
            }
            else{
                engine.setPropagationStrategy(strategy);
            }
            s.setEngine(engine);

            ExecutorService ex = Executors.newSingleThreadExecutor();
            Future<?> f = ex.submit(xscp::solve);
            boolean timedOut = false;
            try{
                f.get(2, TimeUnit.HOURS);
            }
            catch(TimeoutException e){
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
                    xscp.getModel().getSolver().addStopCriterion(()->true);
                }
                ex.shutdownNow();
                ex.awaitTermination(5, TimeUnit.SECONDS);

            }

            if(xscp.monitor){
                res.commit(timedOut);
                handleResults(res, xscp.url);
            }
        }
    }
    private static void handleResults(BenchmarkResults r, String url) throws IOException {
        if(url != null){
            postToUrl(r,url);
        }
        else{
            String js = r.toJSON();
            Path dir = Paths.get(System.getProperty("user.home"), "log");
            Files.createDirectories(dir);
            String name = (r.instance.toString() + r.consistency + r.date)
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            File f = dir.resolve(name).toFile();
            try (BufferedWriter w = Files.newBufferedWriter(f.toPath())) {
                w.write(js);
            }
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
