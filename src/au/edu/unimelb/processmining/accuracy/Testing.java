package au.edu.unimelb.processmining.accuracy;

import java.io.File;
import java.io.PrintWriter;

/**
 * Created by Adriano on 13/07/18.
 */
public class Testing {

    public static void main(String[] args) {
        String modelPath;
        String modelsDir = args[0];
        String logPath = args[1];
        int order = Integer.valueOf(args[2]);
        double prec = 0.0;
        MarkovianAccuracyCalculator macc = new MarkovianAccuracyCalculator();

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(".\\m3_precision_" + System.currentTimeMillis() + ".csv");
            writer.println("log,precision");
        } catch(Exception e) { System.out.println("ERROR - impossible to print the markovian abstraction."); }

        try {
            File dir = new File(modelsDir);
            File[] directoryListing = dir.listFiles();
            if( directoryListing != null ) {
                for( File model : directoryListing ) {
                    modelPath = model.getCanonicalPath();
                    if( modelPath.endsWith(".tsml") ) {
                        try{
                            prec = macc.precision(MarkovianAccuracyCalculator.Abs.MARK, MarkovianAccuracyCalculator.Opd.GRD, logPath, modelPath, order);
                            writer.println(modelPath + "," + prec);
                            writer.flush();
                        } catch( Exception e ) {
                            writer.println(modelPath + ",-1");
                            writer.flush();
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
    }
}
