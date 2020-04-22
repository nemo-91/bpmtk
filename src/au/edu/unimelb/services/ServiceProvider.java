package au.edu.unimelb.services;

import au.edu.qut.processmining.log.ComplexLog;
import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.omega.OmegaMiner;
import au.edu.qut.processmining.miners.splitminer.SplitMiner;
import au.edu.qut.processmining.miners.splitminer.dfgp.DirectlyFollowGraphPlus;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import au.edu.qut.processmining.miners.splitminer.ui.miner.SplitMinerUIResult;
import au.edu.unimelb.processmining.accuracy.MarkovianAccuracyCalculator;
import au.edu.unimelb.processmining.optimization.*;
import com.raffaeleconforti.conversion.bpmn.BPMNToPetriNetConverter;
import com.raffaeleconforti.log.util.LogImporter;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIContext;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.fodina.Fodina;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.processmining.plugins.bpmnminer.types.MinerSettings;
import org.processmining.plugins.kutoolbox.utils.FakePluginContext;
import org.processmining.plugins.pnml.exporting.PnmlExportNetToPNML;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * Created by Adriano on 16/08/18.
 */
public class ServiceProvider {

    public enum TEST_CODE {MAP, MAF, MWT, SMPN, SMD, MAC, AOM, AOL, AORM, OM, OPTF, SMHPO, COMPX, FOD, FOHPO, IMHPO, IMD}

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

        try {
            TEST_CODE code = TEST_CODE.valueOf(args[0]);
            String[] fargs = new String[args.length-1];
            for(int i=1; i<args.length; i++) fargs[i-1] = args[i];

            switch(code) {
    //            case KEN:
    //                (new Testing()).kendallTest(fargs);
    //                break;
    //            case ISL:
    //                testProvider.importSimpleLog8020(fargs);
    //                break;
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
    //            case SMDX:
    //                testProvider.SplitMinerServiceX(fargs);
    //                break;
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
                case OPTF:
                    testProvider.APDO(fargs[0], fargs[1], fargs[2], fargs[3]);
                    break;
                case SMHPO:
                    testProvider.SMHPO(fargs[0]);
                    break;
                case FOHPO:
                    testProvider.FOHPO(fargs[0]);
                    break;
                case IMHPO:
                    testProvider.IMHPO(fargs[0]);
                    break;
                case COMPX:
                    Testing.complexityOnRealModelsSet(fargs[0]);
                    break;
                case FOD:
                    testProvider.FodinaMinerService(fargs);
                    break;
                case IMD:
                    testProvider.InductiveMinerService(fargs);
                    break;
    //            case SMBD:
    //                Testing.SMBatchDiscovery(fargs);
    //                break;
                case SMPN:
                    testProvider.SplitMinerServicePetrinet(fargs);
                    break;
                case MWT:
                    testProvider.Utest(fargs);
                    break;
            }

        } catch(Exception e) {
            int code = Integer.valueOf(args[0]);
            String[] fargs = new String[args.length-1];
            for(int i=1; i<args.length; i++) fargs[i-1] = args[i];

            switch (code) {
                case 1:
                    testProvider.parseComplexLog(fargs);
                    break;
                case 2:
                    testProvider.SplitMiner20Service(fargs);
                    break;
                default: return;
            }
        }
    }

    public void parseComplexLog(String[] args) {
        boolean takeiteasy = false;
        BPMNDiagram diagram;
        SplitMiner sm = new SplitMiner(false, false);

        String logPath = args[0];
        String modelName = args[1] + ".bpmn";

        double eta = Double.valueOf(args[2]);
        double epsilon = Double.valueOf(args[3]);
        boolean parallelismFirst =  Boolean.valueOf(args[4]);
        boolean replaceIORs = Boolean.valueOf(args[5]);
        boolean removeLoopActivities = Boolean.valueOf(args[6]);
//        double auxiliary = Double.valueOf(args[7]);

        try {
            SimpleLog cLog = LogParser.getComplexLog(LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath), new XEventNameClassifier());
            DirectlyFollowGraphPlus dfgp = new DirectlyFollowGraphPlus(cLog, eta, epsilon, DFGPUIResult.FilterType.FWG, parallelismFirst);

            if(takeiteasy && (cLog instanceof ComplexLog)) {
                dfgp.buildDFGfromComplexLog();
                dfgp.detectLoops();
                dfgp.detectParallelismsFromComplexLog();
                diagram = dfgp.convertIntoBPMNDiagramWithOriginalLabels();
            } else {
                dfgp.buildDFGP();
                sm = new SplitMiner(replaceIORs, removeLoopActivities);
                diagram = sm.discoverFromDFGP(dfgp);
            }

            BpmnExportPlugin bpmnExportPlugin = new BpmnExportPlugin();
            UIContext context = new UIContext();
            UIPluginContext uiPluginContext = context.getMainPluginContext();
            bpmnExportPlugin.export(uiPluginContext, diagram, new File(modelName));
            return;
        } catch (Throwable e) {
            System.out.println("ERROR: - something went wrong");
            e.printStackTrace();
            return;
        }
    }

    public void SplitMiner20Service(String[] args) {
        try {
            double epsilon = Double.valueOf(args[0]);
            double eta = Double.valueOf(args[1]);
            boolean replaceIORs = Boolean.valueOf(args[2]);
            XEventClassifier xEventClassifier = new XEventNameClassifier();

            SplitMiner yam = new SplitMiner();
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[3]);
            long etime = System.currentTimeMillis();
            BPMNDiagram output = yam.mineBPMNModel(LogParser.getComplexLog(log, xEventClassifier), xEventClassifier, eta, epsilon, DFGPUIResult.FilterType.FWG, Boolean.valueOf(args[2]), replaceIORs, true, SplitMinerUIResult.StructuringTime.NONE);
            etime = System.currentTimeMillis() - etime;

            System.out.println("eTIME - " + (double)etime/1000.0 + "s");

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
            System.out.println("PARAM: p = [true|false] : replace non trivial OR joins?");
            System.out.println("EXAMPLE: java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider SMD 0.1 0.4 .\\logs\\SEPSIS.xes.gz .\\outputs\\SEPSIS");
            e.printStackTrace();
            return;
        }
    }

    public void Utest(String[] args) {
        String file = args[0];
        int size1 = Integer.valueOf(args[1]);
        int size2 = Integer.valueOf(args[2]);

        double[] best = new double[size1];
        double[] challenger = new double[size2];

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));

            for(int i = 0; i<size1; i++)
                best[i] = Double.parseDouble(reader.readLine());

            for(int i = 0; i<size2; i++)
                challenger[i] = Double.parseDouble(reader.readLine());

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Testing.mannWhitneyTest(best, challenger);
    }

    public void SMHPO(String logPath) {
        SplitMinerHPO smhpo = new SplitMinerHPO();
        smhpo.hyperparamEvaluation(logPath);
    }

    public void FOHPO(String logPath) {
        FodinaHPO fohpo = new FodinaHPO();
        fohpo.hyperparamEvaluation(logPath);
    }

    public void IMHPO(String logPath) {
        InductiveHPO imhpo = new InductiveHPO();
        imhpo.hyperparamEvaluation(logPath);
    }

    public void APDO(String logPath, String order, String metaopt, String miner) {
        AutomatedProcessDiscoveryOptimizer optimizer = new AutomatedProcessDiscoveryOptimizer(Integer.valueOf(order), AutomatedProcessDiscoveryOptimizer.MetaOpt.valueOf(metaopt), MinerProxy.MinerTAG.valueOf(miner));
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
            double eta = Double.valueOf(args[0]);
            double epsilon = Double.valueOf(args[1]);
            boolean parallelismFirst =  Boolean.valueOf(args[2]);
            boolean replaceIORs = Boolean.valueOf(args[3]);
            boolean removeLoopActivities = Boolean.valueOf(args[4]);

            SplitMiner yam = new SplitMiner();
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[5]);
            long etime = System.currentTimeMillis();
            BPMNDiagram output = yam.mineBPMNModel(log, new XEventNameClassifier(), eta, epsilon, DFGPUIResult.FilterType.FWG, parallelismFirst, replaceIORs, removeLoopActivities, SplitMinerUIResult.StructuringTime.NONE);
            etime = System.currentTimeMillis() - etime;

            System.out.println("eTIME - " + (double)etime/1000.0 + "s");

            BpmnExportPlugin bpmnExportPlugin = new BpmnExportPlugin();
            UIContext context = new UIContext();
            UIPluginContext uiPluginContext = context.getMainPluginContext();
            bpmnExportPlugin.export(uiPluginContext, output, new File(args[6] + ".bpmn"));
            return;
        } catch (Throwable e) {
            System.out.println("ERROR: wrong usage.");
            System.out.println("RUN> java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider SMD e n p 'logpath\\log.[xes|xes.gz|mxml]' 'outputpath\\outputname' ");
            System.out.println("PARAM: e = double in [0,1] : parallelism threshold (epsilon)");
            System.out.println("PARAM: n = double in [0,1] : percentile for frequency threshold (eta)");
            System.out.println("PARAM: p = [true|false] : replace non trivial OR joins?");
            System.out.println("EXAMPLE: java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider SMD 0.1 0.4 .\\logs\\SEPSIS.xes.gz .\\outputs\\SEPSIS");
            e.printStackTrace();
            return;
        }
    }

    public void SplitMinerServicePetrinet(String[] args) {

        double epsilon = Double.valueOf(args[0]);
        double eta = Double.valueOf(args[1]);
        boolean replaceIORs = Boolean.valueOf(args[2]);

        SplitMiner yam = new SplitMiner();
        PnmlExportNetToPNML exporter = new PnmlExportNetToPNML();
        BPMNDiagram output;
        Object[] petrinet;
        try{
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[3]);
            long etime = System.currentTimeMillis();
            output = yam.mineBPMNModel(log, new XEventNameClassifier(), eta, epsilon, DFGPUIResult.FilterType.FWG, Boolean.valueOf(args[2]), replaceIORs, true, SplitMinerUIResult.StructuringTime.NONE);
            etime = System.currentTimeMillis() - etime;

            System.out.println("eTIME - " + (double)etime/1000.0 + "s");
            petrinet = BPMNToPetriNetConverter.convert(output);
            exporter.exportPetriNetToPNMLFile(new FakePluginContext(), (Petrinet) petrinet[0], new File(args[4] + ".pnml"));
        } catch (Throwable e) {
            System.out.println("ERROR: wrong usage.");
            System.out.println("RUN (WINDOWS)> java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider SMD e n p 'logpath\\log.[xes|xes.gz|mxml]' 'outputpath\\outputname' ");
            System.out.println("PARAM: e = double in [0,1] : parallelism threshold (epsilon)");
            System.out.println("PARAM: n = double in [0,1] : percentile for frequency threshold (eta)");
            System.out.println("PARAM: p = [true|false] : replace non trivial OR joins?");
            System.out.println("EXAMPLE: java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider SMD 0.1 0.4 .\\logs\\SEPSIS.xes.gz .\\outputs\\SEPSIS");
            e.printStackTrace();
            return;
        }
    }

    public void FodinaMinerService(String[] args) {
        try {
            MinerSettings settings = new MinerSettings();
            settings.dependencyThreshold = Double.valueOf(args[0]);
            settings.l1lThreshold = Double.valueOf(args[1]);
            settings.l2lThreshold = Double.valueOf(args[1]);

            Fodina fodina = new Fodina();
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[2]);

            long etime = System.currentTimeMillis();
            BPMNDiagram output = fodina.discoverBPMNDiagram(LogParser.getSimpleLog(log, new XEventNameClassifier()), settings);
            etime = System.currentTimeMillis() - etime;

            System.out.println("eTIME - " + (double)etime/1000.0 + "s");

            BpmnExportPlugin bpmnExportPlugin = new BpmnExportPlugin();
            UIContext context = new UIContext();
            UIPluginContext uiPluginContext = context.getMainPluginContext();
            bpmnExportPlugin.export(uiPluginContext, output, new File(args[3] + ".bpmn"));
            return;
        } catch (Throwable e) {
            System.out.println("ERROR - fodina couldn't mine the process model.");
            e.printStackTrace();
            return;
        }
    }

    public void InductiveMinerService(String[] args) {
        try {
            IMdProxy iMdProxy = new IMdProxy();
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[0]);

            long etime = System.currentTimeMillis();
            DirectlyFollowGraphPlus dfgp = new DirectlyFollowGraphPlus(LogParser.getSimpleLog(log, new XEventNameClassifier()),0.0,0.0, DFGPUIResult.FilterType.NOF,true);
//            dfgp.buildDFGP();
            dfgp.buildDirectlyFollowsGraph();
            SimpleDirectlyFollowGraph sdfg = new SimpleDirectlyFollowGraph(dfgp, false);
            BPMNDiagram output = iMdProxy.discoverFromSDFG(sdfg);
            etime = System.currentTimeMillis() - etime;

            System.out.println("eTIME - " + (double)etime/1000.0 + "s");

            BpmnExportPlugin bpmnExportPlugin = new BpmnExportPlugin();
            UIContext context = new UIContext();
            UIPluginContext uiPluginContext = context.getMainPluginContext();
            bpmnExportPlugin.export(uiPluginContext, output, new File(args[1] + ".bpmn"));
            return;
        } catch (Throwable e) {
            System.out.println("ERROR - inductive miner couldn't mine the process model.");
            e.printStackTrace();
            return;
        }
    }

    public void SplitMinerServiceX(String[] args) {
        try {
            double epsilon = Double.valueOf(args[0]);
            double eta = Double.valueOf(args[1]);
            boolean replaceIORs = Boolean.valueOf(args[2]);

            SplitMiner yam = new SplitMiner();
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[3]);
            long etime = System.currentTimeMillis();
            LogParser parser = new LogParser();
            XEventNameClassifier classifier = new XEventNameClassifier();
            SimpleLog slog = parser.getSimpleLog(log, classifier, Double.valueOf(args[4]));
            BPMNDiagram output = yam.mineBPMNModel(slog, classifier, eta, epsilon, DFGPUIResult.FilterType.FWG, Boolean.valueOf(args[2]), replaceIORs, false, SplitMinerUIResult.StructuringTime.NONE);
            etime = System.currentTimeMillis() - etime;

            System.out.println("eTIME - " + (double)etime/1000.0 + "s");

            BpmnExportPlugin bpmnExportPlugin = new BpmnExportPlugin();
            UIContext context = new UIContext();
            UIPluginContext uiPluginContext = context.getMainPluginContext();
            String modelName = args[5]+"e"+epsilon+"-n"+eta+"-"+args[2].charAt(0)+"-"+Double.valueOf(args[4])+".bpmn";
            bpmnExportPlugin.export(uiPluginContext, output, new File(modelName));


            MarkovianAccuracyCalculator calculator = new MarkovianAccuracyCalculator();
            calculator.accuracy(MarkovianAccuracyCalculator.Abs.STA, MarkovianAccuracyCalculator.Opd.SPL, args[3], modelName, 5);
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
