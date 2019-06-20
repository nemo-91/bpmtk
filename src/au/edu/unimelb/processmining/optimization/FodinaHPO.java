package au.edu.unimelb.processmining.optimization;

import au.edu.qut.bpmn.metrics.ComplexityCalculator;
import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import au.edu.qut.processmining.miners.splitminer.ui.miner.SplitMinerUIResult;
import au.edu.unimelb.processmining.accuracy.abstraction.LogAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.SubtraceAbstraction;
import com.raffaeleconforti.log.util.LogImporter;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.fodina.Fodina;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.plugins.bpmnminer.types.MinerSettings;

import java.io.*;

public class FodinaHPO {

    private static int MKO = 5;

    private static double l_STEP = 0.05D;
    private static double l_MIN = 0.00D;
    private static double l_MAX = 1.05D;

    private static double d_STEP = 0.05D;
    private static double d_MIN = 0.10D;
    private static double d_MAX = 1.05D;

    public BPMNDiagram hyperparamEvaluation(String logPath) {
        Fodina fodina = new Fodina();
        SimpleLog slog;
        BPMNDiagram bpmn;
        BPMNDiagram bestBPMN = null;
        double[] bestAccuracy = {0.0, 0.0, 0.0};

        ComplexityCalculator complexityCalculator = new ComplexityCalculator();
        SubtraceAbstraction staLog, staProcess;

        double fit;
        double prec;
        Double score;
        int size;
        int cfc;
        double struct;
        long eTime;
        long teTime;

        String combination;
        Double l_threshold;
        Double d_threshold;

        PrintWriter writer;

        String lName;
        lName = logPath.substring(logPath.lastIndexOf("\\")+1);
        lName = lName.substring(0, lName.indexOf("."));
        if(!lName.contains("PRT")) lName = "PUB" + lName;

        try {
            XLog xlog = LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath);
            slog = LogParser.getSimpleLog(xlog, new XEventNameClassifier());
        } catch (Exception e) {
                    e.printStackTrace();
            System.out.println("ERROR - impossible to load the log");
            return null;
        }

        String fName = ".\\fodina_hpo_" + lName + "_" + System.currentTimeMillis() + ".csv";

        try {
            writer = new PrintWriter(fName);
            writer.println("d_threshold,l_threshold,fitness,precision,fscore,size,cfc,struct,mining-time");
        } catch(Exception e) {
            writer = new PrintWriter(System.out);
            System.out.println("ERROR - impossible to create the file for storing the results: printing only on terminal.");
        }

        MinerSettings settings = new MinerSettings();
        staLog = LogAbstraction.subtrace(slog, MKO);

        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {}
        }));

        teTime = System.currentTimeMillis();
        d_threshold = d_MIN;
        do {
            l_threshold = l_MIN;
            do {
                try {

                    eTime = System.currentTimeMillis();
                    settings.dependencyThreshold = d_threshold;
                    settings.l1lThreshold = l_threshold;
                    settings.l2lThreshold = l_threshold;
                    bpmn = fodina.discoverBPMNDiagram(slog, settings);

                    staProcess = SubtraceAbstraction.abstractProcessBehaviour(bpmn, MKO, slog);
                    fit = staLog.minus(staProcess);
                    prec = staProcess.minus(staLog);

                    complexityCalculator.setBPMN(bpmn);
                    size = Integer.valueOf(complexityCalculator.computeSize());
                    cfc = Integer.valueOf(complexityCalculator.computeCFC());
                    struct = Double.valueOf(complexityCalculator.computeStructuredness());

                    score = (fit * prec * 2) / (fit + prec);
                    if( score.isNaN() ) score = -1.0;

                    eTime = System.currentTimeMillis() - eTime;
                    combination = d_threshold + "," + l_threshold + "," + fit + "," + prec + "," + score + "," + size + "," + cfc + "," + struct + "," + ((double)eTime/1000.0);
                    writer.println(combination);
                    writer.flush();

                    if(bestAccuracy[2] < score) {
                        bestAccuracy[0] = fit;
                        bestAccuracy[1] = prec;
                        bestAccuracy[2] = score;
                        bestBPMN = bpmn;
                    }

                } catch (Exception e) {
//                    e.printStackTrace();
                    System.out.println("ERROR - fodina output model broken @ " + d_threshold + " : " + l_threshold);
                }
                l_threshold += l_STEP;
            } while ( l_threshold <= l_MAX);
            d_threshold += d_STEP;
        } while( d_threshold <= d_MAX);

        writer.println("-,-," + bestAccuracy[0] + "," + bestAccuracy[1] + "," + bestAccuracy[2] + ",-,-,-," + (double)(System.currentTimeMillis() - teTime)/1000.0);
        writer.flush();
        writer.close();
        AutomatedProcessDiscoveryOptimizer.exportBPMN(bestBPMN, ".\\fohpo_" + lName + "_best.bpmn");
        return bestBPMN;
    }

}
