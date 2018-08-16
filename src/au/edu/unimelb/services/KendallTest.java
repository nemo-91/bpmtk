package au.edu.unimelb.services;

import au.edu.unimelb.processmining.accuracy.MarkovianAccuracyCalculator;

import java.io.PrintWriter;

/**
 * Created by Adriano on 15/02/18.
 */
public class KendallTest {

    private static String logpath = ".\\";
//    private static String logpath = ".\\PRT";
    private static String modelpath = ".\\";
    private static int MAXO = 5;
    private static int LOG = 1;
    private static MarkovianAccuracyCalculator.Opd MODE = MarkovianAccuracyCalculator.Opd.GRD;

    private static boolean YSM = true;
    private static boolean YIM = false;
    private static boolean YSHM = false;

    public void execute(String args[]) {
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

}
