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

    private ArrayList<Params> paramsRandom;

    public MinerProxy(MinerTAG tag) {
        this.tag = tag;
        ArrayList<Params> params;
        Random random = new Random();
        Params param;

        switch( tag ) {
            case SM:
                sm = new SplitMiner();

                params = new ArrayList<>();
                for(int i=2; i < 6; i++)
                    for(int j=1; j < 4; j++)
                        params.add(new Params(i*0.2, j*0.2));
                paramsRandom = new ArrayList<>(params.size()+1);
                paramsRandom.add(new Params(0.4, 0.1));
                do {
                    param = params.remove(random.nextInt(params.size()));
                    paramsRandom.add(param);
                } while( !params.isEmpty() );
                break;
            default:
                break;
        }
    }

    public void setMinerTAG(MinerTAG tag) { this.tag = tag; }

    public MinerTAG getMinerTAG() { return tag; }

    public SimpleDirectlyFollowGraph restart(SimpleLog slog) {
        DirectlyFollowGraphPlus dfgp;
        Params param;

        switch( tag ) {
            case SM:
                if( paramsRandom.isEmpty() ) return null;
                param = paramsRandom.remove(0);
                dfgp = new DirectlyFollowGraphPlus(slog, param.getParam(0),  param.getParam(1), DFGPUIResult.FilterType.WTH, false);
                dfgp.buildDFGP();
                return new SimpleDirectlyFollowGraph(dfgp);
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
