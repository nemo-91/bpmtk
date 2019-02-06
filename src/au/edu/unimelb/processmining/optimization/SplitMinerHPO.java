package au.edu.unimelb.processmining.optimization;

import au.edu.qut.bpmn.metrics.ComplexityCalculator;
import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.splitminer.SplitMiner;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import au.edu.qut.processmining.miners.splitminer.ui.miner.SplitMinerUIResult;
import au.edu.unimelb.processmining.accuracy.abstraction.LogAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.SubtraceAbstraction;
import com.raffaeleconforti.log.util.LogImporter;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;

import java.io.*;

public class SplitMinerHPO {

    private static int MKO = 5;

    private static double p_STEP = 0.01D;
    private static double p_MIN = 0.00D;
    private static double p_MAX = 1.05D;

    private static double f_STEP = 0.01D;
    private static double f_MIN = 0.10D;
    private static double f_MAX = 1.05D;

    public BPMNDiagram hyperparamEvaluation(String logPath) {
        SplitMiner yam = new SplitMiner();
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
        Double p_threshold;
        Double f_threshold;

        PrintWriter writer;

        String lName;
        lName = logPath.substring(logPath.lastIndexOf("\\")+1);
        lName = lName.substring(0, lName.indexOf("."));
        if(!lName.contains("PRT")) lName = "PUB" + lName;

        try {
            XLog xlog = LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath);
            slog = LogParser.getSimpleLog(xlog, new XEventNameClassifier());
        } catch (Exception e) {
            System.out.println("ERROR - impossible to load the log");
            return null;
        }

        String fName = ".\\splitminer_hpo_" + lName + "_" + System.currentTimeMillis() + ".csv";

        try {
            writer = new PrintWriter(fName);
            writer.println("f_threshold,p_threshold,fitness,precision,fscore,size,cfc,struct,mining-time");
        } catch(Exception e) {
            writer = new PrintWriter(System.out);
            System.out.println("ERROR - impossible to create the file for storing the results: printing only on terminal.");
        }

        staLog = LogAbstraction.subtrace(slog, MKO);
        teTime = System.currentTimeMillis();
        f_threshold = f_MIN;
        do {
            p_threshold = p_MIN;
            do {
                try {
                    System.setOut(new PrintStream(new OutputStream() {
                        @Override
                        public void write(int b) throws IOException {}
                    }));

                    eTime = System.currentTimeMillis();
                    bpmn = yam.mineBPMNModel(slog, new XEventNameClassifier(), f_threshold, p_threshold, DFGPUIResult.FilterType.WTH, false, true, false, SplitMinerUIResult.StructuringTime.NONE);

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
                    combination = f_threshold + "," + p_threshold + "," + fit + "," + prec + "," + score + "," + size + "," + cfc + "," + struct + "," + ((double)eTime/1000.0);
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
                    System.out.println("ERROR - splitminer output model broken @ " + f_threshold + " : " + p_threshold);
                }
                p_threshold += p_STEP;
            } while ( p_threshold <= p_MAX );
            f_threshold += f_STEP;
        } while( f_threshold <= f_MAX );

        writer.println("-,-," + bestAccuracy[0] + "," + bestAccuracy[1] + "," + bestAccuracy[2] + ",-,-,-," + (double)(System.currentTimeMillis() - teTime)/1000.0);
        writer.flush();
        writer.close();
        AutomatedProcessDiscoveryOptimizer.exportBPMN(bestBPMN, ".\\smhpo_" + lName + "_best.bpmn");
        return bestBPMN;
    }

}
