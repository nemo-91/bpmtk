package au.edu.qut.processmining.log;

import org.deckfour.xes.model.XLog;

import java.util.Map;

public class ComplexLog extends SimpleLog {

    private double[] relativeConcurrencyMatrix;
    private double[] relativeDFG;
    private int[] concurrencyMatrix;
    private int[] dfg;
    private int[] exclusiveness;
    private int[] activityObserved;

    private int[] potentialORs;

    public ComplexLog(Map<String, Integer> traces, Map<Integer, String> events, XLog xlog) {
        super(traces, events, xlog);
    }

    public void computePercentages() {
        int totalActivities = getEvents().size();
        relativeConcurrencyMatrix = new double[concurrencyMatrix.length];
        relativeDFG = new double[dfg.length];


        for(int i = 0; i < totalActivities; i++) {
            for (int j = 0; j < totalActivities; j++) {
                relativeDFG[i*totalActivities + j] = (double)dfg[i*totalActivities + j]/activityObserved[i];
                relativeConcurrencyMatrix[i*totalActivities + j] = (double)concurrencyMatrix[i*totalActivities + j]/(activityObserved[i]+activityObserved[j]);
            }
        }

    }

    public double[] getRelativeConcurrencyMatrix(){ return relativeConcurrencyMatrix; }
    public double[] getRelativeDFG(){ return relativeDFG; }

    public int[] getPotentialORs() {
        return potentialORs;
    }
    public void setPotentialORs(int[] potentialORs) {
        this.potentialORs = potentialORs;
    }

    public int[] getActivityObserved() {
        return activityObserved;
    }
    public void setActivityObserved(int[] activityObserved) {
        this.activityObserved = activityObserved;
    }

    public int[] getConcurrencyMatrix() {
        return concurrencyMatrix;
    }
    public void setConcurrencyMatrix(int[] concurrencyMatrix) {
        this.concurrencyMatrix = concurrencyMatrix;
    }

    public int[] getExclusiveness() {
        return exclusiveness;
    }
    public void setExclusiveness(int[] exclusiveness) {
        this.exclusiveness = exclusiveness;
    }

    public int[] getDFG() {
        return dfg;
    }
    public void setDFG(int[] dfg) {
        this.dfg = dfg;
    }

    public void printExclusivenessMatrix() {
        int totalActivities = getEvents().size();

        System.out.print("DEBUG - printing exclusiveness matrix:");
        for(int i = 0; i < totalActivities; i++) {
            System.out.print("\n( ");
            for( int j=0; j < totalActivities; j++) {
                System.out.print(exclusiveness[i*totalActivities + j] + " ");
            }
            System.out.print(")");
        }
        System.out.println();
    }

    public void printConcurrencyMatrix() {
        int totalActivities = getEvents().size();

        System.out.print("DEBUG - printing concurrency matrix:");
        for(int i = 0; i < totalActivities; i++) {
            System.out.print("\n( ");
            for( int j=0; j < totalActivities; j++) {
                System.out.print(concurrencyMatrix[i*totalActivities + j] + " ");
            }
            System.out.print(")");
        }
        System.out.println();
    }

    public void printRelativeConcurrencyMatrix() {
        int totalActivities = getEvents().size();

        System.out.print("DEBUG - printing relative concurrency matrix:");
        for(int i = 0; i < totalActivities; i++) {
            System.out.print("\n( ");
            for( int j=0; j < totalActivities; j++) {
                System.out.print(relativeConcurrencyMatrix[i*totalActivities + j] + " ");
            }
            System.out.print(")");
        }
        System.out.println();
    }

    public void printDFG() {
        int totalActivities = getEvents().size();

        System.out.print("DEBUG - printing DFG matrix:");
        for(int i = 0; i < totalActivities; i++) {
            System.out.print("\n( ");
            for( int j=0; j < totalActivities; j++) {
                System.out.print(dfg[i*totalActivities + j] + " ");
            }
            System.out.print(")");
        }
        System.out.println();
    }

    public void printRelativeDFG() {
        int totalActivities = getEvents().size();

        System.out.print("DEBUG - printing relative DFG matrix:");
        for(int i = 0; i < totalActivities; i++) {
            System.out.print("\n( ");
            for( int j=0; j < totalActivities; j++) {
                System.out.print(relativeDFG[i*totalActivities + j] + " ");
            }
            System.out.print(")");
        }
        System.out.println();
    }

}
