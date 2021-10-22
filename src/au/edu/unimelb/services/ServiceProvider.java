package au.edu.unimelb.services;

import au.edu.qut.bpmn.io.BPMNDiagramImporter;
import au.edu.qut.bpmn.io.impl.BPMNDiagramImporterImpl;
import au.edu.qut.bpmn.metrics.ComplexityCalculator;
import au.edu.qut.processmining.log.ComplexLog;
import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.omega.OmegaMiner;
import au.edu.qut.processmining.miners.splitminer.SplitMiner;
import au.edu.qut.processmining.miners.splitminer.dfgp.DirectlyFollowGraphPlus;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import au.edu.qut.processmining.miners.splitminer.ui.miner.SplitMinerUIResult;
import au.edu.unimelb.processmining.accuracy.MarkovianAccuracyCalculator;
import au.edu.unimelb.processmining.compliance.TimeConstraintsChecker;
import au.edu.unimelb.processmining.optimization.*;
import com.raffaeleconforti.conversion.bpmn.BPMNToPetriNetConverter;
import com.raffaeleconforti.conversion.petrinet.PetriNetToBPMNConverter;
import com.raffaeleconforti.log.util.LogImporter;
import com.raffaeleconforti.marking.MarkingDiscoverer;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIContext;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.fodina.Fodina;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.processmining.plugins.bpmnminer.types.MinerSettings;
import org.processmining.plugins.kutoolbox.utils.FakePluginContext;
import org.processmining.plugins.pnml.exporting.PnmlExportNetToPNML;
import org.processmining.plugins.pnml.importing.PnmlImportNet;
import sun.java2d.pipe.SpanShapeRenderer;

import java.io.*;


/**
 * Created by Adriano on 16/08/18.
 */
public class ServiceProvider {

    public enum TEST_CODE {AVGD, MAP, MAF, SM2, SMPN, SMD, MAC, AOM, AOL, AORM, OPTF, SMHPO, COMPX, FOD, FOHPO, IMHPO, IMD}

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
            System.out.println("TESTCODE - " + args[0]);
            TEST_CODE code = TEST_CODE.valueOf(args[0]);
            String[] fargs = new String[args.length-1];
            for(int i=1; i<args.length; i++) fargs[i-1] = args[i];

            switch(code) {
                case AVGD:
//                    testProvider.averageDistanceLogComplexity(fargs[0]);
                    testProvider.timeConstraintsChecker(fargs[0], fargs[1]);
                    break;
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
//                case OM:
//                    testProvider.omegaMiner(fargs[0]);
//                    break;
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
                    computeComplexity(fargs[0]);
//                    Testing.complexityOnRealModelsSet(fargs[0]);
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
//                    logAnalysis(fargs[0]);
                    testProvider.SplitMinerServicePetrinet(fargs);
                    break;
//                case MWT:
//                    testProvider.Utest(fargs);
//                    break;
                case SM2:
                    testProvider.MineWithSMTC(fargs);
                    break;
            }

        } catch(Exception e) {
            e.printStackTrace();
            int code = Integer.valueOf(args[0]);
            String[] fargs = new String[args.length-1];
            for(int i=1; i<args.length; i++) fargs[i-1] = args[i];

            switch (code) {
                case 1:
                    testProvider.MineWithSMRC(fargs);
                    break;
                case 2:
                    testProvider.SplitMiner20Service(fargs);
                    break;
                case 3:
                    testProvider.SIMMinerService(fargs);
                    break;
                default: return;
            }
        }
    }

    private static void averageDistanceLogComplexity(String logPath) {
        XLog log = null;

        try {
            log = LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath);
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }

        SimpleLog slog = LogParser.getSimpleLog(log, new XEventNameClassifier());
        long etime = System.currentTimeMillis();
        System.out.println("RESULT - " + (new ComplexityCalculator()).logComplexity(slog));
        etime = System.currentTimeMillis() - etime;
        System.out.println("eTIME - " + (double)etime/1000.0 + "s");
    }

    private static void timeConstraintsChecker(String logPath, String rulesPath){
        long etime;
        TimeConstraintsChecker tcc = new TimeConstraintsChecker();

        etime = System.currentTimeMillis();
        tcc.readXLog(logPath);
        etime = System.currentTimeMillis() - etime;
        System.out.println("Loading TIME - " + (double)etime/1000.0 + "s");
//        tcc.print();

        etime = System.currentTimeMillis();
        tcc.checkConstraints(rulesPath);
        etime = System.currentTimeMillis() - etime;
        System.out.println("Querying TIME - " + (double)etime/1000.0 + "s");
    }

    private static void logAnalysis(String logPath) {
        XLog log = null;

        System.out.println("LOGSA - starting analysis ... ");

        try {
            log = LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath);
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }

        SimpleLog slog = LogParser.getComplexLog(log, new XEventNameClassifier());

        System.out.println("LOGSA - total traces: " + slog.size());
        System.out.println("LOGSA - total events: " + slog.getTotalEvents());

        System.out.println("LOGSA - total distinct traces: " + slog.getDistinctTraces());
        System.out.println("LOGSA - total distinct events: " + slog.getDistinctEvents());

        System.out.println("LOGSA - shortest trace length: " + slog.getShortestTrace());
        System.out.println("LOGSA - avg trace length: " + slog.getAvgTraceLength());
        System.out.println("LOGSA - longest trace length: " + slog.getLongestTrace());
    }

    private static void computeComplexity(String modelPath) {
        BPMNDiagramImporter bpmnImporter = new BPMNDiagramImporterImpl();
        ComplexityCalculator complexityCalculator = new ComplexityCalculator();
        BPMNDiagram bpmn;
        PnmlImportNet pnmli = new PnmlImportNet();
        com.raffaeleconforti.context.FakePluginContext fakePluginContext = new com.raffaeleconforti.context.FakePluginContext();
        Petrinet net = null;

        String size;
        String cfc;
        String struct;

            try {
                if(modelPath.contains(".pnml")) {
                    Object o = pnmli.importFile(fakePluginContext, modelPath);
                    if(o instanceof Object[] && (((Object[])o)[0] instanceof Petrinet) ) net = (Petrinet)((Object[])o)[0];
                    else {
                        System.out.println("DEBUG - class: " + o.getClass().getSimpleName());
                        throw new Exception();
                    }

                    Marking initMarking = MarkingDiscoverer.constructInitialMarking(fakePluginContext, net);
                    Marking finalMarking = MarkingDiscoverer.constructFinalMarking(fakePluginContext, net);

                    for(Transition t : net.getTransitions() )
                        if( t.getLabel().matches("t\\d+") || t.getLabel().contains("tau")) t.setInvisible(true);

                    bpmn = PetriNetToBPMNConverter.convert(net, initMarking, finalMarking, false);
                } else bpmn = bpmnImporter.importBPMNDiagram(modelPath);

                complexityCalculator.setBPMN(bpmn);
                size = complexityCalculator.computeSize();
                cfc = complexityCalculator.computeCFC();
                struct = complexityCalculator.computeStructuredness();

                System.out.println("COMPLEXITY (size, CFC, struct.) - (" + size + "," + cfc + "," + struct + ")");
            } catch (Exception e) {
                System.out.println("ERROR - something when wrong with process: " + modelPath);
                e.printStackTrace();
            }
    }

    public void SIMMinerService(String[] args) {
        double eta = Double.valueOf(args[2]);
        double epsilon = Double.valueOf(args[3]);

        try {
            IMdProxy iMdProxy = new IMdProxy();
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[0]);

            long etime = System.currentTimeMillis();
            DirectlyFollowGraphPlus dfgp = new DirectlyFollowGraphPlus(LogParser.getComplexLog(log, new XEventNameClassifier()),eta,epsilon, DFGPUIResult.FilterType.FWG,true);
//            dfgp.buildDFGP();
            dfgp.buildDFGfromComplexLog();
            dfgp.filterWithGuarantees();
            dfgp.addLoops1();
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

    public void MineWithSMRC(String[] args) {
        boolean outputDFG = false;
        boolean filter = false;
        BPMNDiagram diagram;
        SplitMiner sm;

        String logPath = args[0];
        String modelName = args[1] + ".bpmn";

        double eta = Double.valueOf(args[2]);
        double epsilon = Double.valueOf(args[3]);
        boolean parallelismFirst =  Boolean.valueOf(args[4]);
        boolean replaceIORs = Boolean.valueOf(args[5]);
        boolean removeLoopActivities = Boolean.valueOf(args[6]);
//        boolean aux1 = Boolean.valueOf(args[7]);
//        boolean aux2 = Boolean.valueOf(args[8]);
//        outputDFG = aux1;
//        filter = aux2;

        try {
            SimpleLog cLog = LogParser.getComplexLog(LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath), new XEventNameClassifier());
            DirectlyFollowGraphPlus dfgp = new DirectlyFollowGraphPlus(cLog, eta, epsilon, DFGPUIResult.FilterType.FWG, parallelismFirst);

            if(outputDFG && (cLog instanceof ComplexLog)) {
                dfgp.buildDFGfromComplexLog();
                dfgp.detectLoops();
                dfgp.detectParallelismsFromComplexLog();
                if(filter) dfgp.filterWithGuarantees();
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

    public void MineWithSMTC(String[] args) {
        boolean outputDFG = false;
        boolean filter = false;
        BPMNDiagram diagram;
        SplitMiner sm;

        String logPath = args[0];
        String modelName = args[1] + ".bpmn";

        double eta = 1.0;
        double epsilon = Double.valueOf(args[2]);
        boolean parallelismFirst =  true;
        boolean replaceIORs = false;
        boolean removeLoopActivities = false;
//        boolean aux1 = Boolean.valueOf(args[7]);
//        boolean aux2 = Boolean.valueOf(args[8]);
//        outputDFG = aux1;
//        filter = aux2;

        try {
            SimpleLog cLog = LogParser.getComplexLog(LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath), new XEventNameClassifier());
            DirectlyFollowGraphPlus dfgp = new DirectlyFollowGraphPlus(cLog, eta, epsilon, DFGPUIResult.FilterType.FWG, parallelismFirst);

            if(outputDFG && (cLog instanceof ComplexLog)) {
                dfgp.buildDFGfromComplexLog();
                dfgp.detectLoops();
                dfgp.detectParallelismsFromComplexLog();
                if(filter) dfgp.filterWithGuarantees();
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

    public void printDFG(String[] args) {
        try {
            double eta = Double.valueOf(args[1]);
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[0]);
            SimpleLog slog = LogParser.getSimpleLog(log, new XEventNameClassifier());
            long etime = System.currentTimeMillis();
            DirectlyFollowGraphPlus dfgp = new DirectlyFollowGraphPlus(slog, eta, 0.0, DFGPUIResult.FilterType.FWG, false);
            dfgp.buildDirectlyFollowsGraph();
            if(eta > 0) dfgp.filterWithGuarantees();

            etime = System.currentTimeMillis() - etime;

            dfgp.printEdges(true);

            System.out.println("eTIME (excluding printing) - " + (double)etime/1000.0 + "s");

            return;
        } catch (Throwable e) {
            System.out.println("ERROR: incorrect log path.");
            e.printStackTrace();
            return;
        }
    }

    public void SplitMinerService(String[] args) {
        try {
            double eta = Double.valueOf(args[0]);
            double epsilon = Double.valueOf(args[1]);
            boolean parallelismFirst =  Boolean.valueOf(args[2]);
            boolean replaceIORs = Boolean.valueOf(args[3]);
            boolean removeLoopActivityMarkers = Boolean.valueOf(args[4]);

            SplitMiner yam = new SplitMiner();
            XLog log = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[5]);
            long etime = System.currentTimeMillis();
            BPMNDiagram output = yam.mineBPMNModel(log, new XEventNameClassifier(), eta, epsilon, DFGPUIResult.FilterType.FWG, parallelismFirst, replaceIORs, removeLoopActivityMarkers, SplitMinerUIResult.StructuringTime.NONE);
            etime = System.currentTimeMillis() - etime;

            System.out.println("eTIME - " + (double)etime/1000.0 + "s");

            BpmnExportPlugin bpmnExportPlugin = new BpmnExportPlugin();
            UIContext context = new UIContext();
            UIPluginContext uiPluginContext = context.getMainPluginContext();
            bpmnExportPlugin.export(uiPluginContext, output, new File(args[6] + ".bpmn"));
            return;
        } catch (Throwable e) {
            System.out.println("ERROR: wrong usage.");
            System.out.println("RUN> java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider SMD n e p o l 'logpath\\log.[xes|xes.gz|mxml]' 'outputpath\\outputname' ");
            System.out.println("PARAM: e = double in [0,1] : parallelism threshold (epsilon)");
            System.out.println("PARAM: n = double in [0,1] : percentile for frequency threshold (eta)");
            System.out.println("PARAM: p = [true|false] : prioritize parallelism on loops?");
            System.out.println("PARAM: o = [true|false] : replace non trivial OR joins?");
            System.out.println("PARAM: l = [true|false] : remove loop activity markers (false increases model complexity)?");
            System.out.println("EXAMPLE: java -cp bpmtk.jar;lib\\* au.edu.unimelb.services.ServiceProvider SMD 0.1 0.4 .\\logs\\SEPSIS.xes.gz .\\outputs\\SEPSIS");
            e.printStackTrace();
            return;
        }
    }

    public void SplitMinerServicePetrinet(String[] args) {
        PnmlExportNetToPNML exporter = new PnmlExportNetToPNML();
        Object[] petrinet;
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


    public void kendallTest(String[] args) {
        BufferedReader reader;
        double[] series1 = null;
        double[] series2 = null;

        int size = 0;

        System.out.println("DEBUG - running kendall test...");

        try {
            reader = new BufferedReader(new FileReader(args[0]));
            size = Integer.parseInt(reader.readLine()); //first line of the txt file must be the size of the series

            series1 = new double[size];
            series2 = new double[size];

            reader.readLine(); // space between the size and the first series

            for(int i = 0; i<size; i++)
                series1[i] = Double.parseDouble(reader.readLine());

            reader.readLine(); // space between the two series

            for(int i = 0; i<size; i++)
                series2[i] = Double.parseDouble(reader.readLine());

            reader.close();
        } catch (Exception e) {
            System.out.println("ERROR - impossible to load the file for the kendall test");
            e.printStackTrace();
        }

        double kendall = 0.0;
        int discordant = 0;
        int concordant = 0;
        int count = 0;
        int i, j;

        i=0;
        while( i < size ) {
            j = i+1;
            while( j < size ) {
                j++;
                if( series1[i] > series1[j] && series2[i] > series2[j] ) concordant++;
                else if( series1[i] < series1[j] && series2[i] < series2[j] ) concordant++;
                else if( series1[i] == series1[j] && series2[i] == series2[j] ) concordant++;
                else discordant++;
                count++;
            }
            i++;
        }

        kendall = (double)(concordant - discordant)/(double)count;
        System.out.println("RESULT - kendall test value: " + kendall);
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
