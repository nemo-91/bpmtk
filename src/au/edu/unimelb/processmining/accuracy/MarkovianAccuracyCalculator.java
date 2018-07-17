package au.edu.unimelb.processmining.accuracy;

import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.unimelb.processmining.accuracy.abstraction.Abstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.intermediate.AutomatonAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.LogAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.markovian.MarkovAbstraction;
import com.raffaeleconforti.context.FakePluginContext;
import com.raffaeleconforti.log.util.LogImporter;
import de.drscc.automaton.Automaton;
import de.drscc.importer.ImportProcessModel;

import au.edu.unimelb.processmining.accuracy.abstraction.ProcessAbstraction;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.transitionsystem.TransitionSystem;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.tsml.importing.TsmlImportTS;

/**
 * Created by Adriano on 23/01/18.
 */
public class MarkovianAccuracyCalculator {
    public enum Abs {MARK, SET}
    public enum Opd {SPL, HUN, GRD}

    private SimpleLog log;
    private Automaton automaton;
    private AutomatonAbstraction automatonAbstraction;
    private Abstraction logAbstraction, processAbstraction;
    private int order;



    public static void main(String[] args) {
        MarkovianAccuracyCalculator calculator = new MarkovianAccuracyCalculator();
        long start = System.currentTimeMillis();
/*
        if( args.length == 3 ) {
            calculator.accuracy(Abs.MARK, Opd.GRD, args[0], args[1], Integer.valueOf(args[2]));
        } else {
            System.out.println("ERROR - wrong usage.");
            System.out.println("RUN - java -cp \"markovian-accuracy.jar;lib\\*\" au.edu.unimelb.processmining.accuracy.Calculator \".\\log.xes\" \".\\model.pnml\" 3");
        }
/*/
        switch( Abs.valueOf(args[0]) ) {
            case MARK:
                if( args.length == 5 ) calculator.accuracy(Abs.MARK, Opd.valueOf(args[1]), args[2], args[3], Integer.valueOf(args[4]));
                break;
            case SET:
                if( args.length > 2 ) calculator.accuracy(Abs.SET, Opd.valueOf(args[1]), args[2], args[3], 0);
                break;
            default:
                System.out.println("ERROR - wrong precision type.");
        }

        System.out.println("eTIME - " + (System.currentTimeMillis() - start) + "ms");
    }

    public double precision(Abs type, Opd opd, String logP, String processP, int order) {
        return accuracy(type, opd, logP, processP, order)[1];
    }

    public double[] accuracy(Abs type, Opd opd, String logP, String processP, int order) {
        this.order = order;
        double precision = -1;
        double fitness = -1;
        double fscore = -1;

        try {
            if (importLogFromFile(logP, type) && importProcessFromFile(processP, type)) {
                switch(opd) {
                    case SPL:
                        precision = processAbstraction.minus(logAbstraction);
                        fitness = logAbstraction.minus(processAbstraction);
                        break;
                    case HUN:
                        if(processAbstraction instanceof MarkovAbstraction && logAbstraction instanceof MarkovAbstraction)
                        precision = ((MarkovAbstraction)processAbstraction).minusHUN(logAbstraction);
                        break;
                    case GRD:
                        precision = ((MarkovAbstraction)processAbstraction).minusGRD(logAbstraction);
                        break;
                }
            } else {
                System.out.println("ERROR - something went wrong.");
            }

            fscore = (fitness * precision * 2.0) / (fitness + precision);

//            System.out.println("RESULT - fitness: " + fitness);
            System.out.println("RESULT - precision: " + precision);
//            System.out.println("RESULT - f-score: " + fscore);

        } catch(StackOverflowError sofe) {
            precision = 0.0;
            System.out.println("RESULT(e) - precision: " + precision);
//            sofe.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - something went wrong with the GED.");
        }

        double[] accuracy = {fitness, precision, fscore};
        return accuracy;
    }

    public double[] accuracy(Abs type, Opd opd, XLog log, Petrinet petrinet, Marking initialMarking, int order) {
        this.order = order;
        double precision = -1;
        double fitness = -1;
        double fscore = -1;

        try {
            if (importLog(log, type) && importPetrinet(petrinet, initialMarking, type)) {
                switch(opd) {
                    case SPL:
                        precision = processAbstraction.minus(logAbstraction);
                        fitness = logAbstraction.minus(processAbstraction);
                        break;
                    case HUN:
                        if(processAbstraction instanceof MarkovAbstraction && logAbstraction instanceof MarkovAbstraction)
                            precision = ((MarkovAbstraction)processAbstraction).minusHUN(logAbstraction);
                        break;
                    case GRD:
                        precision = ((MarkovAbstraction)processAbstraction).minusGRD(logAbstraction);
                        break;
                }
            } else {
                System.out.println("ERROR - something went wrong.");
            }

            fscore = (fitness * precision * 2.0) / (fitness + precision);

//            System.out.println("RESULT - fitness: " + fitness);
            System.out.println("RESULT - precision: " + precision);
//            System.out.println("RESULT - f-score: " + fscore);

        } catch(StackOverflowError sofe) {
            precision = 0.0;
            System.out.println("RESULT(e) - precision: " + precision);
//            sofe.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - something went wrong with the GED.");
        }

        double[] accuracy = {fitness, precision, fscore};
        return accuracy;
    }

    private boolean importLogFromFile(String lopP, Abs type) {
        XLog xlog;
        System.out.println("INFO - input log: " + lopP);
        try{
            if(!lopP.endsWith(".txt")) {
                xlog = LogImporter.importFromFile(new XFactoryNaiveImpl(), lopP);
                log = LogParser.getSimpleLog(xlog, new XEventNameClassifier());
            } else log = LogParser.getSimpleLog(lopP);

//            for(String s : log.getReverseMap().keySet()) System.out.println("DEBUG - log activity: " + s);

            switch(type) {
                case MARK:
                    logAbstraction = LogAbstraction.markovian(log, order);
                    break;
                case SET:
                    logAbstraction = LogAbstraction.set(log);
                    break;
            }

//            logAbstraction.print();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to read the log file.");
            return false;
        }
    }

    private boolean importLog (XLog xlog, Abs type){
        try{
            log = LogParser.getSimpleLog(xlog, new XEventNameClassifier());
            switch(type) {
                case MARK:
                    logAbstraction = LogAbstraction.markovian(log, order);
                    break;
                case SET:
                    logAbstraction = LogAbstraction.set(log);
                    break;
            }
//            logAbstraction.print();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to read the log file.");
            return false;
        }
    }

    private boolean importProcessFromFile(String processP, Abs type) {
        ImportProcessModel importer = new ImportProcessModel();
        TransitionSystem transitionSystem = null;
        TsmlImportTS tsImporter;
        Object results;
        System.out.println("INFO - input process: " + processP);

        try {
            if( processP.contains(".bpmn") ) automaton = importer.createFSMfromBPNMFileWithConversion(processP, null, null);
            else if(processP.contains(".pnml")) automaton = importer.createFSMfromPNMLFile(processP, null, null);
            else if(processP.contains(".tsml")) {
                tsImporter = new TsmlImportTS();
                results = tsImporter.importFile(new FakePluginContext(), processP);
                if( results instanceof Object[] )
                    if( ((Object[])results)[0] instanceof TransitionSystem ) {
//                        System.out.println("DEBUG - EUREKA");
                        transitionSystem = (TransitionSystem) ((Object[])results)[0];
                    }
            }


            if( transitionSystem == null ) {
//                System.out.println("INFO - D-Automaton (" + automaton.states().size() + "," + automaton.transitions().size() + ")");
                automatonAbstraction = new AutomatonAbstraction(automaton, log);
            } else {
                automatonAbstraction = new AutomatonAbstraction(transitionSystem, log);
//                System.out.println("INFO - TS-Automaton (" + automatonAbstraction.getNodes().size() + "," + automatonAbstraction.getEdges().size() + ")");
            }

            switch(type) {
                case MARK:
                    automatonAbstraction.generateMarkovianLabels(order);
                    processAbstraction = ProcessAbstraction.markovian(automatonAbstraction, 2000000);
                    break;
                case SET:
                    automatonAbstraction.generateSetLabels(log.getReverseMap().size()+1);
                    processAbstraction = ProcessAbstraction.set(automatonAbstraction);
                    break;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to read the process file.");
            return false;
        }
    }


    private boolean importPetrinet(Petrinet petrinet,  Marking initialMarking, Abs type) {
        ImportProcessModel importer = new ImportProcessModel();

        try {
            automaton = importer.createFSMfromPetrinet(petrinet, initialMarking, null, null);
            automatonAbstraction = new AutomatonAbstraction(automaton, log);

            switch(type) {
                case MARK:
                    automatonAbstraction.generateMarkovianLabels(order);
                    processAbstraction = ProcessAbstraction.markovian(automatonAbstraction, 2000000);
                    break;
                case SET:
                    automatonAbstraction.generateSetLabels(log.getReverseMap().size()+1);
                    processAbstraction = ProcessAbstraction.set(automatonAbstraction);
                    break;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to read the process file.");
            return false;
        }
    }



}
