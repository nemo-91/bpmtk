package au.edu.unimelb.processmining.optimization;

import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.splitminer.dfgp.DirectlyFollowGraphPlus;
import au.edu.unimelb.processmining.accuracy.abstraction.LogAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.SubtraceAbstraction;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.Thread.sleep;

public class RepeatedLocalSearch implements Metaheuristics {

    private static int PERTURBATION_STRENGTH = 2;
    private static boolean PERTURBATION = false;

    private MinerProxy minerProxy;

    private SimpleDirectlyFollowGraph currentSDFG;
    private SimpleDirectlyFollowGraph bestSDFG;

    private ArrayList<Double> bestScores;
    private ArrayList<Integer> hits = new ArrayList<>();
    Double[] currentAccuracy = new Double[3];

    private SubtraceAbstraction staLog;
    private SubtraceAbstraction staProcess;

    private int restarts;

    public RepeatedLocalSearch(MinerProxy proxy) {
        minerProxy = proxy;
    }

    public BPMNDiagram searchOptimalSolution(SimpleLog slog, int order, int maxit, int neighbourhood, int timeout) {
        int iterations = 0;
        int icounter = 0;
        restarts = 0;
        staLog = LogAbstraction.subtrace(slog, order);

        ExecutorService multiThreadService;
        Evaluator evalThread;
        Future<Object[]> evalResult;
        Map<SimpleDirectlyFollowGraph, Future<Object[]>> neighboursEvaluations = new HashMap<>();
        String subtrace;
        Set<SimpleDirectlyFollowGraph> neighbours = new HashSet<>();
        Object[] result;

        SimpleDirectlyFollowGraph tmpSDFG;
        BPMNDiagram tmpBPMN;

        boolean improved;


        PrintWriter writer = null;
        try {
            writer = new PrintWriter(".\\ils_results__" + System.currentTimeMillis() + ".csv");
            writer.println("iteration,fitness,precision,fscore,itime");
        } catch(Exception e) { System.out.println("ERROR - impossible to print the markovian abstraction."); }

        long eTime = System.currentTimeMillis();
        long iTime = System.currentTimeMillis();

        if(PERTURBATION) {
            currentSDFG = minerProxy.restart(slog);
            try {
                staProcess = SubtraceAbstraction.abstractProcessBehaviour(minerProxy.getBPMN(currentSDFG), order, slog);
                currentAccuracy[0] = staLog.minus(staProcess);
                currentAccuracy[1] = staProcess.minus(staLog);
                currentAccuracy[2] = (2.0 * currentAccuracy[0] * currentAccuracy[1]) / (currentAccuracy[0] + currentAccuracy[1]);
            } catch(Exception e) {
                System.out.println("ERROR - impossible to generate the initial bpmn.");
                return null;
            }
        } else restart(slog, order);
        bestScores = new ArrayList<>();
        bestScores.add(currentAccuracy[2]);
        hits.add(iterations);
        bestSDFG = currentSDFG;

        while( System.currentTimeMillis() - eTime < timeout && iterations < maxit && currentSDFG != null) {
            try {

                if( currentAccuracy[2] > bestScores.get(bestScores.size()-1) ) {
                    System.out.println("INFO - improved fscore " + currentAccuracy[2]);
                    bestScores.add(currentAccuracy[2]);
                    hits.add(iterations);
                    bestSDFG = currentSDFG;
                }

//                System.out.println("INFO - iteration: " + iterations + " - Q( " + currentAccuracy[2] + " )");
//                System.out.println("INFO - fit & prec : " + currentAccuracy[0] + " & " + currentAccuracy[1]);

                iTime = System.currentTimeMillis() - iTime;
                writer.println(iterations + "," + currentAccuracy[0] + "," + currentAccuracy[1] + "," + currentAccuracy[2] + "," + iTime);
                writer.flush();
                iterations++;
                iTime = System.currentTimeMillis();
//                that's a logical XOR
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
                    do {
                        if( (subtrace = tmpSDFG.enhance(subtrace, 1)) == null ) subtrace = staLog.nextMismatch();
                        else {
                            neighbours.add(tmpSDFG);
                            tmpSDFG = new SimpleDirectlyFollowGraph(currentSDFG);
                        }
                    } while( neighbours.size() != neighbourhood && subtrace != null );

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
                    do {
                        if( (subtrace = tmpSDFG.reduce(subtrace, 1)) == null ) subtrace = staProcess.nextMismatch();
                        else {
                            neighbours.add(tmpSDFG);
                            tmpSDFG = new SimpleDirectlyFollowGraph(currentSDFG);
                        }
                    } while( neighbours.size() != neighbourhood && subtrace != null );

                }

                System.out.println("INFO - selected " + neighbours.size() + " neighbours.");


                multiThreadService = Executors.newFixedThreadPool(neighbours.size());
                for( SimpleDirectlyFollowGraph neighbourSDFG : neighbours ) {
                    try {
                        tmpBPMN = minerProxy.getBPMN(neighbourSDFG);
//                        System.out.println("INFO - created 1 neighbour.");
                    }
                    catch(Exception e) { System.out.println("WARNING - discarded one neighbour."); continue; }
                    evalThread = new Evaluator(staLog, slog, minerProxy, tmpBPMN, order);
                    evalResult = multiThreadService.submit(evalThread);
                    neighboursEvaluations.put(neighbourSDFG, evalResult);
//                    System.out.println("INFO - exploring 1 neighbour.");
                }

                if( neighboursEvaluations.isEmpty() ) {
                    System.out.println("WARNING - empty neighbourhood " + neighbours.size() + " neighbours.");
                    restart(slog, order);
                    continue;
                }

//                System.out.println("INFO - synchronising with threads.");
                sleep(2500);

                improved = false;
                int done = 0;
                int cancelled = 0;
                for( SimpleDirectlyFollowGraph neighbourSDFG : neighboursEvaluations.keySet() ) {
                    evalResult = neighboursEvaluations.get(neighbourSDFG);
                    if( evalResult.isDone() ) {
                        done++;
                        result = evalResult.get();
                        if( (Double)result[2] > currentAccuracy[2] ) {
                            currentAccuracy[0] = (Double)result[0];
                            currentAccuracy[1] = (Double)result[1];
                            currentAccuracy[2] = (Double)result[2];
                            currentSDFG = neighbourSDFG;
                            staProcess = (SubtraceAbstraction) result[3];
                            improved = true;
                            icounter = 0;
                        }
                    } else {
                        cancelled++;
                        evalResult.cancel(true);
                    }
                }

//                System.out.println("DONE - " + done);
//                System.out.println("CANCELLED - " + cancelled);

                neighbours.clear();
                neighboursEvaluations.clear();
                multiThreadService.shutdownNow();

/**     once we checked all the neighbours accuracies, we select the one improving the current state or none at all.
 *      if the one improving the current state, also improves the global maximum, we update that.
 */
                if( !improved && ++icounter == order) {
                    icounter = 0;
                    restart(slog, order);
                }

            } catch (Exception e) {
                System.out.println("ERROR - I got tangled in the threads.");
//                e.printStackTrace();
                restart(slog, order);
            }
        }

        for(int i=0; i<hits.size(); i++)
            writer.println(hits.get(i) + ",-,-," + bestScores.get(i) + ",-");

        System.out.println("eTIME - " + (double)(System.currentTimeMillis() - eTime)/1000.0+ "s");
        System.out.println("STATS - total restarts: " + restarts);

        writer.close();
//        an exception here is impossible.
        try { return minerProxy.getBPMN(bestSDFG); }
        catch(Exception e) { return null; }
    }


    private void restart(SimpleLog slog, int order) {
        Evaluator evaluator;
        ExecutorService executor = null;
        Future<Object[]> evalResult = null;
        BPMNDiagram tmpBPMN;
        Object[] result;


        try {
            restarts++;

            if(PERTURBATION) {
                if(currentSDFG == null) return;
                currentSDFG.perturb(PERTURBATION_STRENGTH);
            } else {
                currentSDFG = minerProxy.restart(slog);
                if(currentSDFG == null) return;
            }

            tmpBPMN = minerProxy.getBPMN(currentSDFG);
            evaluator = new Evaluator(staLog, slog, minerProxy, tmpBPMN, order);
            executor = Executors.newSingleThreadExecutor();
            evalResult = executor.submit(evaluator);

            sleep(2500);
            if( evalResult.isDone() ) {
                result = evalResult.get();
                currentAccuracy[0] = (Double)result[0];
                currentAccuracy[1] = (Double)result[1];
                currentAccuracy[2] = (Double)result[2];
                staProcess = (SubtraceAbstraction) result[3];
                System.out.println("RESTART - done.");
            } else {
                System.out.println("TIMEOUT - restart failed.");
                evalResult.cancel(true);
                executor.shutdownNow();
                restart(slog, order);
            }
        } catch (Exception e) {
            System.out.println("WARNING - restart failed.");
//            e.printStackTrace();
            restart(slog, order);
        }
    }


}
