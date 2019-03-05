package au.edu.unimelb.services;

import au.edu.qut.bpmn.io.BPMNDiagramImporter;
import au.edu.qut.bpmn.io.impl.BPMNDiagramImporterImpl;
import au.edu.qut.bpmn.metrics.ComplexityCalculator;
import au.edu.unimelb.processmining.accuracy.MarkovianAccuracyCalculator;
import au.edu.unimelb.processmining.accuracy.MarkovianAccuracyCalculator.Opd;
import au.edu.unimelb.processmining.accuracy.MarkovianAccuracyCalculator.Abs;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;


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
        boolean petrinet = true;
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
            modelPath = modelsDir + i + "_best.bpmn";
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

}


