package au.edu.unimelb.processmining.optimization;

import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.splitminer.SplitMiner;
import au.edu.qut.processmining.miners.splitminer.dfgp.DirectlyFollowGraphPlus;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import org.processmining.fodina.Fodina;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.plugins.bpmnminer.types.MinerSettings;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class MinerProxy {

    public enum MinerTAG {SM, IM, FO, SHM}
    private MinerTAG tag;
    private SimpleLog slog;

    /****************** Split Miner *******************/
    private SplitMiner sm;
    /***************************************************/

    /****************** Fodina Miner *******************/
    private Fodina fodina;
    private MinerSettings fodinaSettings;
    /***************************************************/

    /***************** Shared SM and FO ****************/
    private ArrayList<Params> restartParams;
    private ArrayList<Params> perturbParams;
    private Params defaultParams;
    /***************************************************/


    public MinerProxy(MinerTAG tag, SimpleLog slog) {
        ArrayList<Params> params;
        Random random = new Random(1);
        Params param;
        Double dparam0;

        this.tag = tag;
        this.slog = slog;

        switch( tag ) {
            case SM:
                sm = new SplitMiner();

                params = new ArrayList<>();
                for(int i=2; i < 6; i++)
                    for(int j=1; j < 4; j++)
                        params.add(new Params(i*0.2, j*0.2));
                restartParams = new ArrayList<>(params.size()+1);

                defaultParams = new Params(0.4, 0.1);
                restartParams.add(defaultParams);
                do {
                    param = params.remove(random.nextInt(params.size()));
                    restartParams.add(param);
                } while( !params.isEmpty() );

                perturbParams = new ArrayList<>(5);
                dparam0 = defaultParams.getParam(0);
                for(int j=0; j < 6; j++)
                    perturbParams.add(new Params(dparam0, j*0.2));
                break;

            case FO:
                System.out.println("DEBUG - Fodina Miner selected.");
                fodina = new Fodina();

                params = new ArrayList<>();
                for(int i=0; i < 11; i++)
                    for(int j=0; j < 11; j++)
                        if( i==j && j==9 ) continue;
                        else params.add(new Params(i*0.1, j*0.1));

                restartParams = new ArrayList<>(params.size()+1);

                defaultParams = new Params(0.9, 0.9);
                restartParams.add(defaultParams);
                do {
                    param = params.remove(random.nextInt(params.size()));
                    restartParams.add(param);
                } while(!params.isEmpty());

                perturbParams = new ArrayList<>(10);
                dparam0 = defaultParams.getParam(0);
                for(int j=0; j < 11; j++)
                    perturbParams.add(new Params(dparam0, j*0.1));

                fodinaSettings = new MinerSettings();
            default:
                break;
        }
    }

    public void setLog(SimpleLog slog) { this.slog = slog; }

    public void setMinerTAG(MinerTAG tag) { this.tag = tag; }

    public MinerTAG getMinerTAG() { return tag; }

    public SimpleDirectlyFollowGraph perturb(SimpleLog slog, SimpleDirectlyFollowGraph sdfg) {
        DirectlyFollowGraphPlus dfgp;
        SimpleDirectlyFollowGraph sdfgo;
        Params param;

        switch( tag ) {
            case SM:
//                if( perturbParams.isEmpty() ) return null;
                param = perturbParams.remove(0);
                perturbParams.add(param);

                dfgp = new DirectlyFollowGraphPlus(slog, param.getParam(0),  param.getParam(1), DFGPUIResult.FilterType.WTH, false);
                dfgp.buildDFGP();
                sdfgo = new SimpleDirectlyFollowGraph(sdfg);
                sdfgo.setParallelisms(dfgp.getParallelisms());
                return sdfgo;
            case FO:
                param = perturbParams.remove(0);
                perturbParams.add(param);

                fodinaSettings.dependencyThreshold = param.getParam(0);
                fodinaSettings.l1lThreshold = param.getParam(1);
                fodinaSettings.l2lThreshold = param.getParam(1);

                return fodina.discoverSDFG(slog, fodinaSettings);
            default:
                return null;
        }
    }

    public SimpleDirectlyFollowGraph restart(SimpleLog slog) {
        DirectlyFollowGraphPlus dfgp;
        SimpleDirectlyFollowGraph sdfg;
        Params param;

        switch( tag ) {
            case SM:
                if( restartParams.isEmpty() ) return null;
                param = restartParams.remove(0);
                dfgp = new DirectlyFollowGraphPlus(slog, param.getParam(0),  param.getParam(1), DFGPUIResult.FilterType.WTH, false);
                dfgp.buildDFGP();
                return new SimpleDirectlyFollowGraph(dfgp, false);
            case FO:
                while(true) {
                    if (restartParams.isEmpty()) return null;
                    param = restartParams.remove(0);

                    fodinaSettings.dependencyThreshold = param.getParam(0);
                    fodinaSettings.l1lThreshold = param.getParam(1);
                    fodinaSettings.l2lThreshold = param.getParam(1);
                    System.out.println("RESTART - fodina with params: " + param.getParam(0) + " - " + param.getParam(1));
                    return fodina.discoverSDFG(slog, fodinaSettings);
/*
                    AsyncFodina afodina = new AsyncFodina(slog, param.getParam(0), param.getParam(1));
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Future<Object[]> sdfgFuture = executor.submit(afodina);
                    try {
                        sleep(60000);
                        if( !sdfgFuture.isDone() ) {
                            sdfgFuture.cancel(true);
                            executor.shutdownNow();
                            continue;
                        } else {
                            sdfg = (SimpleDirectlyFollowGraph) sdfgFuture.get()[0];
                            executor.shutdownNow();
                            System.out.println("DEBUG - done!");
                            return sdfg;
                        }
                    } catch (Exception e) {
                        sdfgFuture.cancel(true);
                        executor.shutdownNow();
                        System.out.println("TIMEOUT - fodina discovery took too long.");
                        continue;
                    }
*/
                }
            default:
                return null;
        }
    }

    public SimpleDirectlyFollowGraph tabuStart(SimpleLog slog) {
        DirectlyFollowGraphPlus dfgp;
        SimpleDirectlyFollowGraph sdfg1;
        SimpleDirectlyFollowGraph sdfg2;
        Params param;

        switch( tag ) {
            case SM:
                dfgp = new DirectlyFollowGraphPlus(slog, 1.0,  1.0, DFGPUIResult.FilterType.WTH, true);
                dfgp.buildDFGP();
                sdfg1 = new SimpleDirectlyFollowGraph(dfgp, true);
//                this is the default param
                param = restartParams.remove(0);
                dfgp = new DirectlyFollowGraphPlus(slog, param.getParam(0),  param.getParam(1), DFGPUIResult.FilterType.WTH, false);
                dfgp.buildDFGP();
                sdfg2 = new SimpleDirectlyFollowGraph(dfgp, false);
                sdfg2.setTabuSet(sdfg1.getTabuSet());
                return sdfg2;
            default:
                return null;
        }
    }

    public BPMNDiagram getBPMN(SimpleDirectlyFollowGraph sdfg) throws Exception {
        switch( tag ) {
            case SM:
                return sm.discoverFromSDFG(sdfg);
            case FO:
                return fodina.discoverFromSDFG(sdfg, slog, fodinaSettings);
            default:
                return null;
        }
    }

    private class Params {
        double[] params;

        private Params(double ... params) { this.params = params.clone(); }
        private double getParam(int index) { return params[index]; }
    }


}
