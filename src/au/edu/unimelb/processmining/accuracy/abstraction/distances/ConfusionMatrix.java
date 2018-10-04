package au.edu.unimelb.processmining.accuracy.abstraction.distances;

import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.Subtrace;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.SubtraceAbstraction;

public class ConfusionMatrix {
    private int logSize, modelSize;
    private Subtrace[] rows; // traces of the log
    private Subtrace[] columns; // traces of the model

    private int[] bestCostsLog; // this array stores the best costs for each log trace
    private int[] bestCostsModel; // this array stores the best costs for each model trace

    private int[] movesOnLog; // this array stores the number of move on log for the min cost alignment of a log trace
    private int[] movesOnModel; // this array stores the number of move on model for the min cost alignment of a model trace

    private int[] syncMovesModel; // this array stores the number of move on model for the min cost alignment of a model trace
    private int[] syncMovesLog;

    private int FP, FN, TP; // false positives, false negatives, and true positives

    public ConfusionMatrix(SubtraceAbstraction log, SubtraceAbstraction model) {
        rows = new Subtrace[logSize = log.getSubtraces().size()];
        columns = new Subtrace[modelSize = model.getSubtraces().size()];

        int r = 0;
        bestCostsLog = new int[logSize];
        for( Subtrace lst : log.getSubtraces() ) {
            rows[r] = lst;
            bestCostsLog[r] = Integer.MAX_VALUE;
            r++;
        }

        int c = 0;
        bestCostsModel = new int[modelSize];
        for( Subtrace mst : model.getSubtraces() ) {
            columns[c] = mst;
            bestCostsModel[c] = Integer.MAX_VALUE;
            c++;
        }

        movesOnLog = new int[logSize];
        movesOnModel = new int[modelSize];

        syncMovesLog = new int[logSize];//????
        syncMovesModel = new int[modelSize];//????

        FP = FN = TP = 0;
    }

    public void compute() {
        int[] alignmentResult;
        int cost, sync, mol, mom;

        for( int r=0; r < logSize; r++ )
            for( int c=0; c < modelSize; c++ ) {
                alignmentResult = Levenshtein.noSwapArrayDistance(rows[r].printIA(), columns[c].printIA());
                cost = alignmentResult[0];
                sync = alignmentResult[1]; //TP
                mol = alignmentResult[2]; //FN
                mom = alignmentResult[3]; //FP

//                we are not optimizing for the least move on model or move on log
                if( bestCostsLog[r] > cost ) {
                    bestCostsLog[r] = cost;
                    movesOnLog[r] = mol;
                    syncMovesLog[r] = sync;
                }

                if( bestCostsModel[c] > cost) {
                    bestCostsModel[c] = cost;
                    movesOnModel[c] = mom;
                    syncMovesModel[c] = sync;
                }
            }

        fillConfusionMatrix();
        print();
    }


    private void fillConfusionMatrix() {
        for( int r=0; r<logSize; r++ ) {
            FN += movesOnLog[r];
            TP += syncMovesLog[r];
        }

        for( int c=0; c<modelSize; c++ ) {
            FP += movesOnModel[c];
            TP += syncMovesModel[c];
        }
    }

    public int getFN() { return FN; }

    public int getFP() { return FP; }

    public int getTP() { return TP; }

    public void print() {
        System.out.println("INFO - FP > " + FP);
        System.out.println("INFO - FN > " + FN);
        System.out.println("INFO - TP > " + TP);
    }
}
