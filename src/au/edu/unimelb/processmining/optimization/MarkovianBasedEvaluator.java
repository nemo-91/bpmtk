package au.edu.unimelb.processmining.optimization;

import au.edu.qut.processmining.log.SimpleLog;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.SubtraceAbstraction;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;

import java.util.concurrent.Callable;

public class MarkovianBasedEvaluator implements Callable<Object[]> {

    private SimpleLog slog;
    private SubtraceAbstraction staLog;
    private MinerProxy proxy;
    private BPMNDiagram bpmn;
//    private SimpleDirectlyFollowGraph sdfg;
    private int order;

    public MarkovianBasedEvaluator(SubtraceAbstraction staLog, SimpleLog slog, MinerProxy minerProxy, BPMNDiagram bpmn, int order) {
        this.staLog = staLog;
        this.proxy = minerProxy;
        this.bpmn = bpmn;
        this.order = order;
        this.slog = slog;
    }

    @Override
    public Object[] call() throws Exception {
        SubtraceAbstraction staProcess;
        Object[] results = new Object[5];

        try {
//            bpmn = proxy.getBPMN(sdfg);
            staProcess = SubtraceAbstraction.abstractProcessBehaviour(this.bpmn, order, slog);

            if(staProcess == null) return new Object[]{new Double(0.0), new Double(0.0), new Double(0.0), null};

            results[0] = new Double(staLog.minus(staProcess));
            results[1] = new Double(staProcess.minus(staLog));
            results[2] = new Double((2.0 * (Double)results[0] * (Double)results[1]) / ((Double)results[0] + (Double)results[1]));
            results[2] = (Double)results[0] == Double.NaN ? 0.0 : results[2];
            results[3] = staProcess;
            results[4] = this.bpmn;
        } catch(Exception e) {
            results[0] = results[1] = results[2] = new Double(0.0);
            results[3] = null;
            results[4] = this.bpmn;
        } catch(Error e) {
            results[0] = results[1] = results[2] = new Double(0.0);
            results[3] = null;
            results[4] = this.bpmn;
        }

//        System.out.println("INFO - thread done, accuracy: [" + results[0] + "," + results[1] + "," + results[2] + "]");
        return results;
    }

}
