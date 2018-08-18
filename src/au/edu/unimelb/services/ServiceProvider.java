package au.edu.unimelb.services;

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

    public enum TEST_CODE {KEN, MAC, SMT}

    public static void main(String[] args) {

        TEST_CODE code = TEST_CODE.valueOf(args[0]);
        String[] fargs = new String[args.length-1];
        for(int i=1; i<args.length; i++) fargs[i-1] = args[i];

        ServiceProvider testProvider = new ServiceProvider();

        switch(code) {
            case KEN:
                (new KendallTest()).execute(fargs);
                break;
            case MAC:
                testProvider.MarkovianAccuracyTest(fargs);
                break;
            case SMT:
                testProvider.SplitMinerTest(fargs);
                break;
        }

    }

    public void MarkovianAccuracyTest(String[] args) {
        MarkovianAccuracyCalculator calculator = new MarkovianAccuracyCalculator();
        long start = System.currentTimeMillis();
/*
        if( args.length == 3 ) {
            calculator.accuracy(Abs.MARK, Opd.GRD, args[0], args[1], Integer.valueOf(args[2]));
        } else {
            System.out.println("ERROR - wrong usage.");
            System.out.println("RUN - java -cp \"markovian-accuracy.jar;lib\\*\" au.edu.unimelb.processmining.accuracy.Calculator \".\\log.xes\" \".\\model.pnml\" 3");
        }
/*/
        switch( MarkovianAccuracyCalculator.Abs.valueOf(args[0]) ) {
            case MARK:
                if( args.length == 5 ) calculator.accuracy(MarkovianAccuracyCalculator.Abs.MARK, MarkovianAccuracyCalculator.Opd.valueOf(args[1]), args[2], args[3], Integer.valueOf(args[4]));
                break;
            case SET:
                if( args.length > 2 ) calculator.accuracy(MarkovianAccuracyCalculator.Abs.SET, MarkovianAccuracyCalculator.Opd.valueOf(args[1]), args[2], args[3], 0);
                break;
            default:
                System.out.println("ERROR - wrong precision type.");
        }

        System.out.println("eTIME - " + (System.currentTimeMillis() - start) + "ms");
    }

    public void SplitMinerTest(String[] args) {
        try {
            double epsilon = Double.valueOf(args[0]);
            double eta = Double.valueOf(args[1]);

            SplitMiner yam = new SplitMiner();
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[2]);
            BPMNDiagram output = yam.mineBPMNModel(log, new XEventNameClassifier(), eta, epsilon, DFGPUIResult.FilterType.FWG, true, true, true, SplitMinerUIResult.StructuringTime.NONE);

            BpmnExportPlugin bpmnExportPlugin = new BpmnExportPlugin();
            UIContext context = new UIContext();
            UIPluginContext uiPluginContext = context.getMainPluginContext();
            bpmnExportPlugin.export(uiPluginContext, output, new File(args[3] + ".bpmn"));
            return;
        } catch (Throwable e) {
            System.out.println("ERROR: wrong usage.");
            System.out.println("USAGE: java -jar splitminer.jar e n 'logpath\\log.[xes|xes.gz|mxml]' 'outputpath\\outputname' ");
            System.out.println("PARAM: e = double in [0,1] : parallelism threshold (epsilon)");
            System.out.println("PARAM: n = double in [0,1] : percentile for frequency threshold (eta)");
            System.out.println("EXAMPLE: java -jar splitminer.jar 0.1 0.4 .\\logs\\SEPSIS.xes.gz .\\outputs\\SEPSIS");
            e.printStackTrace();
            return;
        }
    }

}
