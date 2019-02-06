package au.edu.unimelb.processmining.optimization;

import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.splitminer.SplitMiner;
import au.edu.qut.processmining.miners.splitminer.dfgp.DirectlyFollowGraphPlus;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;

import java.util.ArrayList;
import java.util.Random;

public class MinerProxy {

    public enum MinerTAG {SM, IM, FO, SHM}
    private MinerTAG tag;

    private SplitMiner sm;

    private ArrayList<Params> restartParams;
    private ArrayList<Params> perturbParams;
    private Params defaultParams;

    public MinerProxy(MinerTAG tag) {
        this.tag = tag;
        ArrayList<Params> params;
        Random random = new Random(1);
        Params param;
        Double dparam0;

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
            default:
                break;
        }
    }

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
            default:
                return null;
        }
    }

    public SimpleDirectlyFollowGraph restart(SimpleLog slog) {
        DirectlyFollowGraphPlus dfgp;
        Params param;

        switch( tag ) {
            case SM:
                if( restartParams.isEmpty() ) return null;
                param = restartParams.remove(0);
                dfgp = new DirectlyFollowGraphPlus(slog, param.getParam(0),  param.getParam(1), DFGPUIResult.FilterType.WTH, false);
                dfgp.buildDFGP();
                return new SimpleDirectlyFollowGraph(dfgp, false);
            default:
                return null;
        }
    }

    public SimpleDirectlyFollowGraph tabuStart(SimpleLog slog) {
        DirectlyFollowGraphPlus dfgp;
        Params param;

        switch( tag ) {
            case SM:
                dfgp = new DirectlyFollowGraphPlus(slog, 1.0,  0.5, DFGPUIResult.FilterType.WTH, false);
                dfgp.buildDFGP();
                return new SimpleDirectlyFollowGraph(dfgp, true);
            default:
                return null;
        }
    }

    public BPMNDiagram getBPMN(SimpleDirectlyFollowGraph sdfg) throws Exception {
        switch( tag ) {
            case SM:
                return sm.discoverFromSDFG(sdfg);
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
