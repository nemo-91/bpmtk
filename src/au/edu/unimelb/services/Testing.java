package au.edu.unimelb.services;

import au.edu.qut.bpmn.io.BPMNDiagramImporter;
import au.edu.qut.bpmn.io.impl.BPMNDiagramImporterImpl;
import au.edu.qut.bpmn.metrics.ComplexityCalculator;
import au.edu.qut.processmining.miners.splitminer.SplitMiner;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import au.edu.qut.processmining.miners.splitminer.ui.miner.SplitMinerUIResult;
import au.edu.unimelb.processmining.accuracy.MarkovianAccuracyCalculator;
import au.edu.unimelb.processmining.accuracy.MarkovianAccuracyCalculator.Opd;
import au.edu.unimelb.processmining.accuracy.MarkovianAccuracyCalculator.Abs;
import com.raffaeleconforti.conversion.bpmn.BPMNToPetriNetConverter;
import com.raffaeleconforti.log.util.LogImporter;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIContext;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.processmining.plugins.kutoolbox.utils.FakePluginContext;
import org.processmining.plugins.pnml.exporting.PnmlExportNetToPNML;


import java.io.File;
import java.io.PrintWriter;

/**
 * Created by Adriano on 15/02/18.
 */
public class Testing {

    private static String logpath = ".\\";
//    private static String logpath = ".\\PRT";
    private static String modelpath = ".\\";
    private static int MAXO = 5;
    private static int LOG = 1;
    private static MarkovianAccuracyCalculator.Opd MODE = MarkovianAccuracyCalculator.Opd.GRD;

    private static boolean YSM = true;
    private static boolean YIM = false;
    private static boolean YSHM = false;

    public void kendallTest(String args[]) {
        String smM, imM, shmM, log;
        int o;
        double kendall_one = 0.0;
        double kendall_two = 0.0;
        double kendall_three = 0.0;
        double time;

        double[] imp = new double[14];
        double[] smp = new double[14];
        double[] shmp = new double[14];


        double[] imp_avgtime = new double[14];
        double[] smp_avgtime = new double[14];
        double[] shmp_avgtime = new double[14];

        MarkovianAccuracyCalculator calculator = new MarkovianAccuracyCalculator();
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(".\\precision_" + System.currentTimeMillis() + ".csv");
            writer.println("log,miner,order,precision,etime");
        } catch(Exception e) { System.out.println("ERROR - impossible to print the markovian abstraction."); }


        for( int i=LOG; i<13; i++ ) {
            log = logpath + i + ".xes.gz";
            smM = modelpath + "sm\\" + i + ".pnml";
            shmM = modelpath + "shm\\" + i + ".pnml";
            imM = modelpath + "im\\" + i + ".pnml";
            o = 0;
            try {
                while(o<MAXO) {
                    if(YSM) {
                        System.out.println("INFO - kendall order " + (o + 1));
                        time = System.currentTimeMillis();
                        smp[o] = calculator.precision(MarkovianAccuracyCalculator.Abs.MARK, MODE, log, smM, o + 1);
                        time = (System.currentTimeMillis() - time) / 1000.00;
                        System.out.println("TIME - " + time);
                        smp_avgtime[o] += time;
                        writer.println(log + ",SM," + (o + 1) + "," + smp[o] + "," + time);
                        writer.flush();
                    }

                    if(YSHM) {
                        time = System.currentTimeMillis();
                        shmp[o] = calculator.precision(MarkovianAccuracyCalculator.Abs.MARK, MODE, log, shmM, o + 1);
                        time = (System.currentTimeMillis() - time) / 1000.00;
                        System.out.println("TIME - " + time);
                        shmp_avgtime[o] += time;
                        writer.println(log + ",SHM," + (o + 1) + "," + shmp[o] + "," + time);
                        writer.flush();
                    }

                    if(YIM) {
                        time = System.currentTimeMillis();
                        imp[o] = calculator.precision(MarkovianAccuracyCalculator.Abs.MARK, MODE, log, imM, o + 1);
                        time = (System.currentTimeMillis() - time) / 1000.00;
                        System.out.println("TIME - " + time);
                        imp_avgtime[o] += time;
                        writer.println(log + ",IM," + (o + 1) + "," + imp[o] + "," + time);
                        writer.flush();
                    }

                    o++;
                }
                o--;
//                kendall_one = evaluateKendall(smp, imp, o);
//                kendall_two = evaluateKendall(smp, shmp, o);
//                kendall_three = evaluateKendall(shmp, imp, o);
            } catch (Exception e) {
//                kendall_one = evaluateKendall(smp, imp, o);
//                kendall_two = evaluateKendall(smp, shmp, o);
//                kendall_three = evaluateKendall(shmp, imp, o);
            } catch (Error e) {
//                kendall_one = evaluateKendall(smp, imp, o);
//                kendall_two = evaluateKendall(smp, shmp, o);
//                kendall_three = evaluateKendall(shmp, imp, o);
            }
//            writer.println(log + ",SM,IM,kendall," + kendall_one);
//            writer.println(log + ",SM,SHM,kendall," + kendall_two);
//            writer.println(log + ",SHM,IM,kendall," + kendall_three);
            writer.flush();
        }
        writer.close();
    }


    static public void SMBatchDiscovery(String[] args) {
            int startIndex = Integer.valueOf(args[1]);
            int endIndex = Integer.valueOf(args[2]) + 1;

            double epsilon = 0.1;
            double eta = 0.4;
            boolean replaceIORs = true;

            String logPath = "";
            String logsDir = args[0];

            SplitMiner yam = new SplitMiner();
            PnmlExportNetToPNML exporter = new PnmlExportNetToPNML();
            XLog log;
            BPMNDiagram output;
            Object[] petrinet;

            for (int i = startIndex; i < endIndex; i++) {
                try {
                    logPath = logsDir + i + ".xes";
                    log = LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath);
                    output = yam.mineBPMNModel(log, new XEventNameClassifier(), eta, epsilon, DFGPUIResult.FilterType.FWG, Boolean.valueOf(args[2]), replaceIORs, true, SplitMinerUIResult.StructuringTime.NONE);
                    petrinet = BPMNToPetriNetConverter.convert(output);
                    exporter.exportPetriNetToPNMLFile(new FakePluginContext(), (Petrinet) petrinet[0], new File(logPath + ".pnml"));
                } catch (Exception e) {
//                    e.printStackTrace();
                    System.out.println("ERROR - impossible to discover a petri net from: " + logPath);
                    continue;
                } catch (Error e) {
//                    e.printStackTrace();
                    System.out.println("ERROR - impossible to discover a petri net from: " + logPath);
                    continue;
                }
            }
    }


    private static double evaluateKendall(double[] smp, double imp[], int o) {
        double kendall = 0.0;
        int discordant = 0;
        int concordant = 0;
        int count = 0;
        int i, j;

        i=0;
        while( i<o ) {
            j = i;
            while( j<o ) {
                j++;
                if( smp[i] > imp[i] && smp[j] > imp[j] ) concordant++;
                else if( smp[i] < imp[i] && smp[j] < imp[j] ) concordant++;
                else if( smp[i] == imp[i] && smp[j] == imp[j] ) concordant++;
                else discordant++;
                count++;
            }
            i++;
        }

        kendall = (double)(concordant - discordant)/(double)count;
        return kendall;
    }

    public static void accuracyOnModelsSet(Abs aType, Opd oType, String modelsDir, String logPath, int maxOrder) {
        MarkovianAccuracyCalculator calculator = new MarkovianAccuracyCalculator();
        double[] accuracy;
        long[] time;
        String modelPath;
        PrintWriter writer = null;
        int order;

        long eTime = System.currentTimeMillis();

        try {
            writer = new PrintWriter(".\\" + aType.toString() + "_" + oType.toString() + "o2-" + maxOrder + "_" + System.currentTimeMillis() + ".csv");
            writer.println("log,order,fitness,precision,f-score,etime-fit, etime-prec");
        } catch(Exception e) { System.out.println("ERROR - impossible to print the markovian the results."); }

        try {
            File dir = new File(modelsDir);
            File[] directoryListing = dir.listFiles();
            if( directoryListing != null ) {
                for( File model : directoryListing ) {
                    modelPath = model.getCanonicalPath();
                    if( modelPath.endsWith(".pnml") ){
                        order = 2;
                        while( order < maxOrder ) {
                            try {
                                accuracy = calculator.accuracy(aType, oType, logPath, modelPath, order);
                                time = calculator.getExecutionTime();
                                writer.println(modelPath + "," + order + "," + accuracy[0] + "," + accuracy[1] + "," + accuracy[2] + "," + (time[0]+time[3]) + "," + (time[1]+time[3]));
                                writer.flush();
                                order++;
                            } catch (Exception e) {
                                break;
                            } catch (Error e) {
                                break;
                            }
                        }
                    }
                }
            } else {
                System.out.println("ERROR - input path not a directory.");
                return;
            }
        } catch ( Exception e ) {
            System.out.println("ERROR - " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println("INFO - total testing time: " + (System.currentTimeMillis() - eTime));
        writer.close();
    }

    public static void accuracyOnLogsSet(Abs aType, Opd oType, String modelPath, String logsDir, int maxOrder) {
        MarkovianAccuracyCalculator calculator = new MarkovianAccuracyCalculator();
        double[] accuracy;
        long[] time;
        PrintWriter writer = null;
        String logPath;
        int order;

        long eTime = System.currentTimeMillis();

        try {
            writer = new PrintWriter(".\\" + aType.toString() + "_" + oType.toString() + "o2-" + (maxOrder-1) + "_" + System.currentTimeMillis() + ".csv");
            writer.println("log,order,fitness,precision,f-score,etime-fit, etime-prec");
        } catch(Exception e) { System.out.println("ERROR - impossible to print the markovian the results."); }

        try {
            File dir = new File(logsDir);
            File[] directoryListing = dir.listFiles();
            if( directoryListing != null ) {
                for( File log : directoryListing ) {
                    logPath = log.getCanonicalPath();
                    if( logPath.endsWith(".xes.gz") ) {
                        order = 2;
                        while( order < maxOrder ) {
                            try {
                                accuracy = calculator.accuracy(aType, oType, logPath, modelPath, order);
                                time = calculator.getExecutionTime();
                                writer.println(logPath + "," + order + "," + accuracy[0] + "," + accuracy[1] + "," + accuracy[2] + "," + (time[0]+time[3]) + "," + (time[1]+time[3]));
                                writer.flush();
                                order++;
                            } catch (Exception e) {
                                break;
                            } catch (Error e) {
                                break;
                            }
                        }
                    }
                }
            } else {
                System.out.println("ERROR - input path not a directory.");
                return;
            }
        } catch ( Exception e ) {
            System.out.println("ERROR - " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println("INFO - total testing time: " + (System.currentTimeMillis() - eTime));
        writer.close();
    }

    public static void accuracyOnRealModelsSet(Abs aType, Opd oType, String modelsDir, String logsDir, int maxOrder) {
        MarkovianAccuracyCalculator calculator = new MarkovianAccuracyCalculator();
        double[] accuracy = null;
        double measure = 0.0;
        long[] time;
        String modelPath;
        String logPath;
        PrintWriter writer = null;
        int order;
//        switch this flag to get either fitness or precision or fscore (i.e. all of them)
        boolean fscore = true;
        boolean petrinet = false;
        String print;

        long eTime = System.currentTimeMillis();

        try {
            writer = new PrintWriter(".\\" + aType.toString() + "_" + oType.toString() + "o2-" + maxOrder + "_" + System.currentTimeMillis() + ".csv");
            writer.println("log,order,fitness,precision,f-score,etime-fit, etime-prec");
        } catch(Exception e) { System.out.println("ERROR - impossible to print the markovian the results."); }

        try {
            for(int i = 1; i<13; i++) {
                if(petrinet) modelPath = modelsDir + i + ".pnml";
                else modelPath = modelsDir + i + ".bpmn";
                logPath = logsDir + i + ".xes.gz";
                if( logPath.contains("PRT") && (i==5 || i==8 || i>10) ) continue;

                order = 5;
                while (order <= maxOrder) {
                    try {
                        if(fscore) {
                            accuracy = calculator.accuracy(aType, oType, logPath, modelPath, order);
                            time = calculator.getExecutionTime();
                            print = modelPath + "," + order + "," + accuracy[0] + "," + accuracy[1] + "," + accuracy[2] + "," + (time[0] + time[3]) + "," + (time[1] + time[3]);
                            System.out.println("WRITER - " + print);
                            writer.println(print);
                        } else {
                            measure = calculator.fitness(aType, oType, logPath, modelPath, order);
                            time = calculator.getExecutionTime();
                            writer.println(modelPath + "," + order + "," + measure + ",0,0," + (time[0] + time[3]) + ",-");
                        }
                        writer.flush();
                        order++;
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    } catch (Error e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        } catch ( Exception e ) {
            System.out.println("ERROR - " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println("INFO - total testing time: " + (System.currentTimeMillis() - eTime));
        writer.close();
    }


    public static void complexityOnRealModelsSet(String modelsDir) {
        BPMNDiagramImporter bpmnImporter = new BPMNDiagramImporterImpl();
        ComplexityCalculator complexityCalculator = new ComplexityCalculator();
        String modelPath;
        BPMNDiagram bpmn;
        PrintWriter writer;

        String size;
        String cfc;
        String struct;

        try {
            writer = new PrintWriter(".\\complexity_" + System.currentTimeMillis() + ".csv");
            writer.println("model,size,cfc,struct.");
        } catch(Exception e) {
            System.out.println("ERROR - impossible to print the markovian the results.");
            writer = new PrintWriter(System.out);
        }

        for(int i = 1; i<13; i++) {
            modelPath = modelsDir + i + ".bpmn";
            if( modelPath.contains("PRT") && (i==5 || i==8 || i>10) ) continue;

            try {
                bpmn = bpmnImporter.importBPMNDiagram(modelPath);
                complexityCalculator.setBPMN(bpmn);
                size = complexityCalculator.computeSize();
                cfc = complexityCalculator.computeCFC();
                struct = complexityCalculator.computeStructuredness();

                writer.println(modelPath + "," + size + "," + cfc + "," + struct);
                writer.flush();
            } catch (Exception e) {
                System.out.println("ERROR - something when wrong with process: " + modelPath);
                e.printStackTrace();
                continue;
            }
        }
        writer.close();
    }

    static public double mannWhitneyTest(double[] best, double[] challenger) {
        int n1 = (best.length);
        int n2 = (challenger.length);

        double u1 = 0.0;
        double u2 = 0.0;
        double uo;

        double z;

        System.out.println("DEBUG - n1 = " + n1);
        System.out.println("DEBUG - n2 = " + n2);
        n1--;
        n2--;
        System.out.println("DEBUG - " + best[n1]);
        System.out.println("DEBUG - " + challenger[n2]);


        for(int i = 0; i<n1; i++)
            for(int j=0; j<n2; j++) {
                if (best[i] > challenger[j]) u1 += 1.0;
                else if (best[i] == challenger[j]) u1 += 0.5;
            }

        for(int i = 0; i<n2; i++)
            for(int j=0; j<n1; j++) {
                if (challenger[i] > best[j]) u2 += 1.0;
                else if (challenger[i] == best[j]) u2 += 0.5;
            }

        n1++;
        n2++;
        System.out.println("DEBUG - n1*n2 = " + (n1*n2));
        System.out.println("DEBUG - U1 = " + u1);
        System.out.println("DEBUG - U2 = " + u2);
        System.out.println("DEBUG - U1 + U2 = " + (u1 + u2));

        uo = u1 > u2 ? u2 : u1;
        double x = (double)n1*n2;
        double y = n1+n2+1.0;

        System.out.println("DEBUG - X = " + x);
        System.out.println("DEBUG - Y = " + y);

        z = (uo - (x/2.0))/Math.sqrt(x*y/12.0);

        System.out.println("RESULT - z = " + z);
        if(z > 1.96 || z < -1.96) System.out.println("DEBUG - statistically significant.");

        return z;
    }

}


