package au.edu.unimelb.services;

import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.miners.omega.OmegaMiner;
import au.edu.qut.processmining.miners.splitminer.SplitMiner;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import au.edu.qut.processmining.miners.splitminer.ui.miner.SplitMinerUIResult;
import au.edu.unimelb.processmining.accuracy.MarkovianAccuracyCalculator;
import au.edu.unimelb.processmining.optimization.AutomatedProcessDiscoveryOptimizer;
import au.edu.unimelb.processmining.optimization.MinerProxy;
import com.raffaeleconforti.log.util.LogImporter;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIContext;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;

import java.io.File;


/**
 * Created by Adriano on 16/08/18.
 */
public class ServiceProvider {

    public enum TEST_CODE {KEN, MAP, MAF, SMD, ISL, MAC, AOM, AOL, AORM, OM, BPM19}

    public static void main(String[] args) {
        ServiceProvider testProvider = new ServiceProvider();


//        if( args.length != 5) printHelp();
//        args[4] = Integer.toString(Integer.valueOf(args[4]) + 1);
//
//        switch(TEST_CODE.valueOf(args[0])) {
//            case MAP:
//                args[0] = "STA";
//                testProvider.MarkovianPrecisionService(args);
//                break;
//            case MAF:
//                args[0] = "STA";
//                testProvider.MarkovianFitnessService(args);
//                break;
//            case MAC:
//                args[0] = "STA";
//                testProvider.MarkovianAccuracyService(args);
//                break;
//            default:
//                printHelp();
//                return;
//        }

        TEST_CODE code = TEST_CODE.valueOf(args[0]);
        String[] fargs = new String[args.length-1];
        for(int i=1; i<args.length; i++) fargs[i-1] = args[i];

        switch(code) {
            case KEN:
                (new Testing()).kendallTest(fargs);
                break;
            case MAP:
                testProvider.MarkovianPrecisionService(fargs);
                break;
            case MAF:
                testProvider.MarkovianFitnessService(fargs);
                break;
            case MAC:
                testProvider.MarkovianAccuracyService(fargs);
                break;
            case SMD:
                testProvider.SplitMinerService(fargs);
                break;
            case ISL:
                testProvider.importSimpleLog8020(fargs);
                break;
            case AOM:
                Testing.accuracyOnModelsSet(MarkovianAccuracyCalculator.Abs.valueOf(fargs[0]), MarkovianAccuracyCalculator.Opd.valueOf(fargs[1]), fargs[2], fargs[3], Integer.valueOf(fargs[4]));
                break;
            case AOL:
                Testing.accuracyOnLogsSet(MarkovianAccuracyCalculator.Abs.valueOf(fargs[0]), MarkovianAccuracyCalculator.Opd.valueOf(fargs[1]), fargs[2], fargs[3], Integer.valueOf(fargs[4]));
                break;
            case AORM:
                Testing.accuracyOnRealModelsSet(MarkovianAccuracyCalculator.Abs.valueOf(fargs[0]), MarkovianAccuracyCalculator.Opd.valueOf(fargs[1]), fargs[2], fargs[3], Integer.valueOf(fargs[4]));
                break;
            case OM:
                testProvider.omegaMiner(fargs[0]);
                break;
            case BPM19:
                testProvider.APDO(fargs[0], fargs[1]);
                break;
        }

    }

    public void APDO(String logPath, String order) {
        AutomatedProcessDiscoveryOptimizer optimizer = new AutomatedProcessDiscoveryOptimizer(Integer.valueOf(order), AutomatedProcessDiscoveryOptimizer.MetaOpt.RLS, MinerProxy.MinerTAG.SM);
        optimizer.init(logPath);
        optimizer.searchOptimalBPMN();
    }

    public void omegaMiner(String logPath) {
        new OmegaMiner().mineAndExport(logPath);
    }

    public void MarkovianAccuracyService(String[] args) {
        MarkovianAccuracyCalculator calculator = new MarkovianAccuracyCalculator();
        long start = System.currentTimeMillis();

        if( args.length == 5 ) {
            calculator.accuracy(MarkovianAccuracyCalculator.Abs.valueOf(args[0]), MarkovianAccuracyCalculator.Opd.valueOf(args[1]), args[2], args[3], Integer.valueOf(args[4]));
        } else {
            System.out.println("ERROR - wrong usage.");
            System.out.println("RUN> java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider MAC GRD log-path model-path 3");
        }

        System.out.println("eTIME - " + (System.currentTimeMillis() - start) + "ms");
    }

    public void MarkovianPrecisionService(String[] args) {
        MarkovianAccuracyCalculator calculator = new MarkovianAccuracyCalculator();
        long start = System.currentTimeMillis();

        if( args.length == 5 ) {
            calculator.precision(MarkovianAccuracyCalculator.Abs.valueOf(args[0]), MarkovianAccuracyCalculator.Opd.valueOf(args[1]), args[2], args[3], Integer.valueOf(args[4]));
        } else {
            System.out.println("ERROR - wrong usage.");
            System.out.println("RUN> java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider MAC GRD log-path model-path 3");
        }

        System.out.println("eTIME - " + (System.currentTimeMillis() - start) + "ms");
    }

    public void MarkovianFitnessService(String[] args) {
        MarkovianAccuracyCalculator calculator = new MarkovianAccuracyCalculator();
        long start = System.currentTimeMillis();

        if( args.length == 5 ) {
            calculator.fitness(MarkovianAccuracyCalculator.Abs.valueOf(args[0]), MarkovianAccuracyCalculator.Opd.valueOf(args[1]), args[2], args[3], Integer.valueOf(args[4]));
        } else {
            System.out.println("ERROR - wrong usage.");
            System.out.println("RUN> java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider MAF GRD log-path model-path 3");
        }

        System.out.println("eTIME - " + (System.currentTimeMillis() - start) + "ms");
    }

    public void SplitMinerService(String[] args) {
        try {
            double epsilon = Double.valueOf(args[0]);
            double eta = Double.valueOf(args[1]);

            SplitMiner yam = new SplitMiner();
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[3]);
            BPMNDiagram output = yam.mineBPMNModel(log, new XEventNameClassifier(), eta, epsilon, DFGPUIResult.FilterType.FWG, Boolean.valueOf(args[2]), true, true, SplitMinerUIResult.StructuringTime.NONE);

            BpmnExportPlugin bpmnExportPlugin = new BpmnExportPlugin();
            UIContext context = new UIContext();
            UIPluginContext uiPluginContext = context.getMainPluginContext();
            bpmnExportPlugin.export(uiPluginContext, output, new File(args[4] + ".bpmn"));
            return;
        } catch (Throwable e) {
            System.out.println("ERROR: wrong usage.");
            System.out.println("RUN> java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider SMD e n p 'logpath\\log.[xes|xes.gz|mxml]' 'outputpath\\outputname' ");
            System.out.println("PARAM: e = double in [0,1] : parallelism threshold (epsilon)");
            System.out.println("PARAM: n = double in [0,1] : percentile for frequency threshold (eta)");
            System.out.println("PARAM: p = [true|false] : parallelisms are discovered before loops");
            System.out.println("EXAMPLE: java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider SMD 0.1 0.4 .\\logs\\SEPSIS.xes.gz .\\outputs\\SEPSIS");
            e.printStackTrace();
            return;
        }
    }

    public void importSimpleLog8020(String[] args) {
        LogParser logParser = new LogParser();
        try {
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[1]);
            logParser.getSimpleLog(log, new XEventNameClassifier(), Double.valueOf(args[0]));
        } catch (Exception e) {
            System.out.println("ERROR - impossible to import the log");
            e.printStackTrace();
        }
    }

    static private void printHelp() {
        System.out.println("ERROR: wrong usage.");
        System.out.println("RUN> java -cp markovian-accuracy.jar;lib\\* au.edu.unimelb.services.ServiceProvider C F 'logpath\\log.[xes|xes.gz|mxml]' 'modelpath\\model.[bpmn|pnml]' K ");
        System.out.println("PARAM: C = operative code, either one of the following: MAC | MAP | MAF");
        System.out.println("PARAM: F = cost function, either one of the following: SPL | HUN");
        System.out.println("PARAM: K = Markovian abstraction order, an integer greater/equal than 2, best is 3 to 5");
        System.out.println("EXAMPLE: java -cp markovian-accuracy.jar;lib\\* au.edu.unimelb.services.ServiceProvider MAC SPL .\\logs\\original.mxml .\\model1.pnml 4");
    }

}
