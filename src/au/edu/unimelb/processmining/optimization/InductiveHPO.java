package au.edu.unimelb.processmining.optimization;

import au.edu.qut.bpmn.metrics.ComplexityCalculator;
import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.unimelb.processmining.accuracy.abstraction.LogAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.SubtraceAbstraction;
import com.raffaeleconforti.conversion.petrinet.PetriNetToBPMNConverter;
import com.raffaeleconforti.log.util.LogImporter;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.plugins.pnml.exporting.PnmlExportNetToPNML;
import au.edu.qut.bpmn.helper.Petrinet2BPMNConverter;

import java.io.*;

public class InductiveHPO {

    private static int MKO = 4;

    private static float f_STEP = 0.05F;
    private static float f_MIN = 0.00F;
    private static float f_MAX = 1.01F;

    public BPMNDiagram hyperparamEvaluation(String logPath) {
        PnmlExportNetToPNML exporter = new PnmlExportNetToPNML();
        IMPetriNet inductive = new IMPetriNet();
        MiningParametersIMf imParams = new MiningParametersIMf();
        Object[] result;
        XLog xlog;
        SimpleLog slog;

        Petrinet bestPN = null;
        BPMNDiagram bestBPMN = null;
        double[] bestAccuracy = {0.0, 0.0, 0.0};

        ComplexityCalculator complexityCalculator = new ComplexityCalculator();
        SubtraceAbstraction staLog, staProcess;

        double fit;
        double prec;
        Double score;
        int size = 0;
        int cfc = 0;
        double struct = 0;
        long eTime;
        long teTime;

        String combination;
        float f_threshold;

        PrintWriter writer;

        String lName;
        lName = logPath.substring(logPath.lastIndexOf("\\")+1);
        lName = lName.substring(0, lName.indexOf("."));
        if(!lName.contains("PRT")) lName = "PUB" + lName;

        try {
            xlog = LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath);
            slog = LogParser.getSimpleLog(xlog, new XEventNameClassifier());
        } catch (Exception e) {
                    e.printStackTrace();
            System.out.println("ERROR - impossible to load the log");
            return null;
        }

        String fName = ".\\im_hpo_" + lName + "_" + System.currentTimeMillis() + ".csv";

        try {
            writer = new PrintWriter(fName);
            writer.println("f_threshold,fitness,precision,fscore,size,cfc,struct,mining-time");
        } catch(Exception e) {
            writer = new PrintWriter(System.out);
            System.out.println("ERROR - impossible to create the file for storing the results: printing only on terminal.");
        }

        staLog = LogAbstraction.subtrace(slog, MKO);

//        System.setOut(new PrintStream(new OutputStream() {
//            @Override
//            public void write(int b) throws IOException {}
//        }));

        teTime = System.currentTimeMillis();
            f_threshold = f_MIN;
            do {
                try {

                    eTime = System.currentTimeMillis();
                    imParams.setNoiseThreshold(f_threshold);
                    result = inductive.minePetriNetParameters(new org.processmining.plugins.kutoolbox.utils.FakePluginContext(), xlog, imParams);

                    staProcess = SubtraceAbstraction.abstractProcessBehaviour((Petrinet) result[0], (Marking) result[1], MKO, slog);
                    fit = staLog.minus(staProcess);
                    prec = staProcess.minus(staLog);

                    score = (fit * prec * 2) / (fit + prec);
                    if( score.isNaN() ) score = -1.0;

                    eTime = System.currentTimeMillis() - eTime;
                    combination = + f_threshold + "," + fit + "," + prec + "," + score + "," + size + "," + cfc + "," + struct + "," + ((double)eTime/1000.0);
                    writer.println(combination);
                    writer.flush();

                    if(bestAccuracy[2] < score) {
                        bestAccuracy[0] = fit;
                        bestAccuracy[1] = prec;
                        bestAccuracy[2] = score;
                        bestPN = (Petrinet) result[0];
                    }

                    System.out.println("DEBUG - hello " + f_threshold);
                } catch (Exception e) {
//                    e.printStackTrace();
                    System.out.println("ERROR - im output model broken @ " + f_threshold);
                }
                f_threshold += f_STEP;
                System.out.println("DEBUG - f_threshold: " + f_threshold);
            } while ( f_threshold <= f_MAX);

        try {
            complexityCalculator.setBPMN(bestBPMN = Petrinet2BPMNConverter.getBPMN(bestPN));
            AutomatedProcessDiscoveryOptimizer.exportBPMN(bestBPMN, ".\\imhpo_" + lName + "_best.bpmn");
            size = Integer.valueOf(complexityCalculator.computeSize());
            cfc = Integer.valueOf(complexityCalculator.computeCFC());
            struct = Double.valueOf(complexityCalculator.computeStructuredness());
        } catch(Exception e) {
            System.out.println("ERROR - impossible to compute complexity!");
        }

        writer.println("-,-," + bestAccuracy[0] + "," + bestAccuracy[1] + "," + bestAccuracy[2] + "," + size + "," + cfc + "," + struct + "," + (double)(System.currentTimeMillis() - teTime)/1000.0);
        writer.flush();
        writer.close();

        try {
            exporter.exportPetriNetToPNMLFile(new org.processmining.plugins.kutoolbox.utils.FakePluginContext(), bestPN, new File(".\\imhpo_" + lName + "_.pnml"));
        } catch( Exception e) {
            System.out.println("ERROR - impossible to export petrinet!");
        }

        return bestBPMN;
    }

}
