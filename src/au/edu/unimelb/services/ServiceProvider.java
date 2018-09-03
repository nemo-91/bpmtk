package au.edu.unimelb.services;

import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.miners.splitminer.SplitMiner;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import au.edu.qut.processmining.miners.splitminer.ui.miner.SplitMinerUIResult;
import au.edu.unimelb.processmining.accuracy.MarkovianAccuracyCalculator;
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

    public enum TEST_CODE {KEN, MAP, MAF, SMD, ISL, MAC, AOM, AORM}

    public static void main(String[] args) {

        TEST_CODE code = TEST_CODE.valueOf(args[0]);
        String[] fargs = new String[args.length-1];
        for(int i=1; i<args.length; i++) fargs[i-1] = args[i];

        ServiceProvider testProvider = new ServiceProvider();

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
            case AORM:
                Testing.accuracyOnRealModelsSet(MarkovianAccuracyCalculator.Abs.valueOf(fargs[0]), MarkovianAccuracyCalculator.Opd.valueOf(fargs[1]), fargs[2], fargs[3], Integer.valueOf(fargs[4]));
                break;
        }

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

}
