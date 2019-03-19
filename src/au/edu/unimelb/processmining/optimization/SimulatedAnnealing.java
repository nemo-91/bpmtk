package au.edu.unimelb.processmining.optimization;

import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.splitminer.dfgp.DirectlyFollowGraphPlus;
import au.edu.unimelb.processmining.accuracy.abstraction.LogAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.SubtraceAbstraction;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.Thread.sleep;

public class SimulatedAnnealing implements Metaheuristics {

//    this represents the percentage of total iteration that must past before the temperature reaches 0
    private static double TSP = 0.80;

    private MinerProxy minerProxy;

    private BPMNDiagram currentBPMN;
    private BPMNDiagram bestBPMN;

    private SimpleDirectlyFollowGraph currentSDFG;
    private SimpleDirectlyFollowGraph bestSDFG;

    private ArrayList<Double> bestScores;
    private ArrayList<Integer> hits;
    Double[] currentAccuracy = new Double[3];

    private SubtraceAbstraction staLog;
    private SubtraceAbstraction staProcess;

    private PrintWriter writer;
    private int restarts;

    public SimulatedAnnealing(MinerProxy proxy) {
        minerProxy = proxy;
    }

    public BPMNDiagram searchOptimalSolution(SimpleLog slog, int order, int maxit, int neighbourhood, int timeout, String modelName) {
        int iterations = 0;
        double exponent;
        double probability;
        int maxTemperature = (int)(maxit*TSP);
        int icounter = 0;
        boolean improved;
        boolean export = false;

        staLog = LogAbstraction.subtrace(slog, order);

        ExecutorService multiThreadService;
        MarkovianBasedEvaluator evalThread;
        Future<Object[]> evalResult;
        Map<SimpleDirectlyFollowGraph, Future<Object[]>> neighboursEvaluations = new HashMap<>();
        String subtrace;
        Set<SimpleDirectlyFollowGraph> neighbours = new HashSet<>();
        Object[] result;

        SimpleDirectlyFollowGraph tmpSDFG;
        BPMNDiagram tmpBPMN;

        hits = new ArrayList<>();
        bestScores = new ArrayList<>();

        writer = null;
        try {
            writer = new PrintWriter(".\\sima_" + modelName + ".csv");
            writer.println("iteration,fitness,precision,fscore,itime");
        } catch(Exception e) { System.out.println("ERROR - impossible to print the markovian abstraction."); }

        long eTime = System.currentTimeMillis();
        long iTime = System.currentTimeMillis();

        restart(slog, order);
        bestScores.add(currentAccuracy[2]);
        hits.add(iterations);
        bestSDFG = currentSDFG;
        bestBPMN = currentBPMN;

        while( System.currentTimeMillis() - eTime < timeout && iterations < maxit && currentSDFG != null) {
            try {

                if( currentAccuracy[2] > bestScores.get(bestScores.size()-1) ) {
                    System.out.println("INFO - improved fscore " + currentAccuracy[2]);
                    bestScores.add(currentAccuracy[2]);
                    hits.add(iterations);
                    bestSDFG = currentSDFG;
                    bestBPMN = currentBPMN;
                }

                iTime = System.currentTimeMillis() - iTime;
                if(export) AutomatedProcessDiscoveryOptimizer.exportBPMN(currentBPMN, ".\\sima_" + modelName + "_" + iterations + ".bpmn");
                writer.println(iterations + "," + currentAccuracy[0] + "," + currentAccuracy[1] + "," + currentAccuracy[2] + "," + iTime);
                writer.flush();
                iterations++;
                iTime = System.currentTimeMillis();


                if( currentAccuracy[1] > currentAccuracy[0] ) {
/**     if precision is higher than fitness, we explore the DFGs having more edges.
 *      to do so, we select the most frequent edges of the markovian abstraction of the log that do not appear
 *      in the markovian abstraction of the process, NOTE: each edge is a subtrace.
 *      we select C*N subtraces and we add C subtraces at a time to a copy of the current DFG.
 *      each of this copy is considered to be a neighbour of the current DFG with an improved fitness.
 *      for each of this copy we compute the f-score, and we retain the one with highest f-score.
 **/
                    staLog.computeDifferences(staProcess);
                    subtrace = staLog.nextMismatch();
                    tmpSDFG = new SimpleDirectlyFollowGraph(currentSDFG);
                    while( neighbours.size() != neighbourhood && subtrace != null ) {
                        if( subtrace.isEmpty() && (subtrace = staLog.nextMismatch()) == null ) break;

                        if( (subtrace = tmpSDFG.enhance(subtrace, 1)) == null ) subtrace = staLog.nextMismatch();
                        else {
                            neighbours.add(tmpSDFG);
                            tmpSDFG = new SimpleDirectlyFollowGraph(currentSDFG);
                        }
                    }

                } else {
/**     if fitness is higher than precision, we explore the DFGs having less edges.
 *      to do so, we select random edges of the markovian abstraction of the process that do not appear
 *      in the markovian abstraction of the log.
 *      we select C*N subtraces and we add C subtraces at a time to a copy of the current DFG.
 *      each of this copy is considered to be a neighbour of the current DFG with an improved fitness.
 *      for each of this copy we compute the f-score, and we retain the one with highest f-score.
 **/
                    staProcess.computeDifferences(staLog);
                    subtrace = staProcess.nextMismatch();
                    tmpSDFG = new SimpleDirectlyFollowGraph(currentSDFG);
                    while( neighbours.size() != neighbourhood && subtrace != null ) {
                        if( subtrace.isEmpty() && (subtrace = staProcess.nextMismatch()) == null ) break;

                        if( (subtrace = tmpSDFG.reduce(subtrace, 1)) == null ) subtrace = staProcess.nextMismatch();
                        else {
                            neighbours.add(tmpSDFG);
                            tmpSDFG = new SimpleDirectlyFollowGraph(currentSDFG);
                        }
                    }
                }

//                System.out.println("INFO - selected " + neighbours.size() + " neighbours.");

                if( neighbours.isEmpty() ) {
//                    System.out.println("WARNING - empty neighbourhood " + neighbours.size() + " neighbours.");
                    restart(slog, order);
                    continue;
                }

                multiThreadService = Executors.newFixedThreadPool(neighbours.size());
                for( SimpleDirectlyFollowGraph neighbourSDFG : neighbours ) {
                    try { tmpBPMN = minerProxy.getBPMN(neighbourSDFG); }
                    catch(Exception e) { System.out.println("WARNING - discarded one neighbour."); continue; }
                    evalThread = new MarkovianBasedEvaluator(staLog, slog, minerProxy, tmpBPMN, order);
                    evalResult = multiThreadService.submit(evalThread);
                    neighboursEvaluations.put(neighbourSDFG, evalResult);
//                    System.out.println("INFO - exploring 1 neighbour.");
                }

//                System.out.println("INFO - synchronising with threads.");
                sleep(2500);
                improved = false;
                for( SimpleDirectlyFollowGraph neighbourSDFG : neighboursEvaluations.keySet() ) {
                    evalResult = neighboursEvaluations.get(neighbourSDFG);
                    if( evalResult.isDone() ) {
                        result = evalResult.get();
                        if( (Double)result[2] > currentAccuracy[2] ) {
                            currentAccuracy[0] = (Double)result[0];
                            currentAccuracy[1] = (Double)result[1];
                            currentAccuracy[2] = (Double)result[2];
                            staProcess = (SubtraceAbstraction) result[3];
                            currentBPMN = (BPMNDiagram) result[4];
                            currentSDFG = neighbourSDFG;
                            improved = true;
                            icounter = 0;
                        } else if( iterations != maxTemperature && result[3] != null && icounter != 0) {
                            exponent = (currentAccuracy[2] - (Double)result[2])/(maxTemperature - iterations);
                            probability = Math.exp(exponent);
                            if( probability > 0 ) {
//                                System.out.println("INFO - annealing with probability: " + probability);
                                currentAccuracy[0] = (Double)result[0];
                                currentAccuracy[1] = (Double)result[1];
                                currentAccuracy[2] = (Double)result[2];
                                staProcess = (SubtraceAbstraction) result[3];
                                currentBPMN = (BPMNDiagram) result[4];
                                currentSDFG = neighbourSDFG;
                                icounter = 0;
                                writer.println("s,s,s,s,s");
//                                break;
                            }
                        }
                    } else evalResult.cancel(true);
                }

/**     once we checked all the neighbours accuracies, we select the one improving the current state or none at all.
 *      if the one improving the current state, also improves the global maximum, we update that.
 */
                neighbours.clear();
                neighboursEvaluations.clear();
                multiThreadService.shutdownNow();

                if( !improved && ++icounter == order) {
                    icounter = 0;
                    restart(slog, order);
                }

            } catch (Exception e) {
                System.out.println("ERROR - I got tangled in the threads.");
                e.printStackTrace();
                restart(slog, order);
            }
        }

        eTime = System.currentTimeMillis() - eTime;
        String hitrow = "";
        String fscorerow = "";
        for(int i=0; i<hits.size(); i++) {
            hitrow +=  hits.get(i) + ",";
            fscorerow += bestScores.get(i) + ",";
        }

        writer.println(hitrow + (double)(eTime)/1000.0);
        writer.println(fscorerow + (double)(eTime)/1000.0);
        writer.close();

        System.out.println("eTIME - " + (double)(eTime)/1000.0+ "s");
//        System.out.println("STATS - total restarts: " + restarts);

        return bestBPMN;
    }


    private void restart(SimpleLog slog, int order) {
        MarkovianBasedEvaluator markovianBasedEvaluator;
        ExecutorService executor = null;
        Future<Object[]> evalResult;
        BPMNDiagram tmpBPMN;
        Object[] result;


        try {
            restarts++;
            currentSDFG = minerProxy.restart(slog);
            if(currentSDFG == null) return;

            tmpBPMN = minerProxy.getBPMN(currentSDFG);
            markovianBasedEvaluator = new MarkovianBasedEvaluator(staLog, slog, minerProxy, tmpBPMN, order);
            executor = Executors.newSingleThreadExecutor();
            evalResult = executor.submit(markovianBasedEvaluator);

            sleep(2500);
            if( evalResult.isDone() ) {
                result = evalResult.get();
                currentAccuracy[0] = (Double)result[0];
                currentAccuracy[1] = (Double)result[1];
                currentAccuracy[2] = (Double)result[2];
                staProcess = (SubtraceAbstraction) result[3];
                currentBPMN = (BPMNDiagram) result[4];
                executor.shutdownNow();
//                System.out.println("RESTART - done.");
                writer.println("r,r,r,r,r");
            } else {
//                System.out.println("TIMEOUT - restart failed.");
                evalResult.cancel(true);
                executor.shutdownNow();
                restart(slog, order);
            }
        } catch (Exception e) {
//            System.out.println("WARNING - restart failed.");
//            e.printStackTrace();
            if(executor != null) executor.shutdownNow();
            restart(slog, order);
        }
    }
}
