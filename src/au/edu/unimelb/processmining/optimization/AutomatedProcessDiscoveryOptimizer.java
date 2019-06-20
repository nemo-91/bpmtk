package au.edu.unimelb.processmining.optimization;

import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import com.raffaeleconforti.log.util.LogImporter;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIContext;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;

import java.io.File;

public class AutomatedProcessDiscoveryOptimizer {

    private static int MAXIT = 50;
    private static int NEIGHBOURHOOD = 5;
    private static int TIMEOUT = 300000;

    public enum MetaOpt {RLS, ILS, TS, SA}
    private MinerProxy minerProxy;
    private int order;
    private MetaOpt metaheuristics;
    private MinerProxy.MinerTAG miner;
    private SimpleLog slog;
    private BPMNDiagram bpmn;

    private String modelName;

    private Metaheuristics explorer;

    public AutomatedProcessDiscoveryOptimizer(int order, MetaOpt metaheuristics, MinerProxy.MinerTAG mtag) {
        this.order = order;
        this.metaheuristics = metaheuristics;
        this.miner = mtag;
    }

    public boolean init(String logPath) {
        XLog xlog;
        modelName = logPath.substring(logPath.lastIndexOf("\\")+1);
        modelName = modelName.substring(0, modelName.indexOf("."));
        if(!modelName.contains("PRT")) modelName = "PUB" + modelName;

//        System.out.println("MODEL NAME = " + modelName);

        try {
            xlog = LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath);
            slog = LogParser.getSimpleLog(xlog, new XEventNameClassifier());
        } catch (Exception e) {
            System.out.println("ERROR - impossible to load the log");
            slog = null;
            return false;
        }
        minerProxy = new MinerProxy(miner, slog);
        return true;
    }

    public BPMNDiagram searchOptimalBPMN() {

        switch(metaheuristics) {
            case RLS:
                explorer = new RepeatedLocalSearch(minerProxy);
                bpmn = explorer.searchOptimalSolution(slog, order, MAXIT, NEIGHBOURHOOD, TIMEOUT, modelName);
                break;
            case ILS:
                explorer = new IteratedLocalSearch(minerProxy);
                bpmn = explorer.searchOptimalSolution(slog, order, MAXIT, NEIGHBOURHOOD, TIMEOUT, modelName);
                break;
            case TS:
                explorer = new TabuSearch(minerProxy);
                bpmn = explorer.searchOptimalSolution(slog, order, MAXIT, NEIGHBOURHOOD, TIMEOUT, modelName);
                break;
            case SA:
                explorer = new SimulatedAnnealing(minerProxy);
                bpmn = explorer.searchOptimalSolution(slog, order, MAXIT, NEIGHBOURHOOD, TIMEOUT, modelName);
                break;
        }

//        exportBPMN(bpmn, ".\\os-bpmn_" + System.currentTimeMillis() + ".bpmn");
        exportBPMN(bpmn, ".\\" + metaheuristics.toString() + "_" + modelName + "_best.bpmn");
        return  bpmn;
    }

    protected static void exportBPMN(BPMNDiagram diagram, String path) {
        BpmnExportPlugin bpmnExportPlugin = new BpmnExportPlugin();
        UIContext context = new UIContext();
        UIPluginContext uiPluginContext = context.getMainPluginContext();
        try {
            bpmnExportPlugin.export(uiPluginContext, diagram, new File(path));
        } catch (Exception e) { System.out.println("ERROR - impossible to export the BPMN"); }
    }
}
