package au.edu.unimelb.processmining.decomposition;

import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.unimelb.processmining.accuracy.abstraction.Abstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.LogAbstraction;
import com.raffaeleconforti.log.util.LogCloner;
import com.raffaeleconforti.log.util.LogImporter;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.jbpt.hypergraph.abs.IVertex;
import org.processmining.plugins.log.exporting.ExportLogXesGz;
import org.processmining.stagemining.algorithms.AbstractStageMining;
import org.processmining.stagemining.algorithms.StageMiningHighestModularity;
import org.processmining.stagemining.models.DecompositionTree;
import org.processmining.stagemining.utils.LogUtilites;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Adriano on 19/04/18.
 */
public class Decomposer {

    public static final int MINSIZE = 3;

    private LogCloner cloner;

    private XLog xlog;
    private SimpleLog slog;

    private Map<String, String> eventsMapping;

    private Map<Integer, Map<Integer,Set<String>>> clusters;
    private Map<Integer, Map<Integer,XLog>> logsHierarchy;
    private Map<Integer, Map<Integer,String>> logsDetails;

    public static void main(String[] args) {
        XLog xlog;
        Decomposer decomposer;

        try {
            xlog = LogImporter.importFromFile(new XFactoryNaiveImpl(), args[0]);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - provide the log path as first parameter.");
            return;
        }

        decomposer = new Decomposer();
        decomposer.generateLogsHierarchy(xlog, MINSIZE, true, args[1]);
    }

    public Decomposer() {}

    public Map<Integer, Map<Integer,XLog>> generateLogsHierarchy(XLog xlog, int size, boolean export, String path) {
        init(xlog);
        clusterize(decompose(cloner.cloneLog(xlog), size));
        if(export) exportLogs(path);
        return logsHierarchy;
    }

    private void init(XLog xlog) {
        cloner = new LogCloner();
        logsHierarchy = new HashMap<>();
        logsDetails = new HashMap<>();

        this.xlog = xlog;
        slog = LogParser.getSimpleLog(xlog, new XEventNameClassifier());

        eventsMapping = new HashMap<>();
        for( String a : slog.getReverseMap().keySet() ) eventsMapping.put(a.toLowerCase(), a);
    }

    private DecompositionTree decompose(XLog log, int size) {
        long eTime;
        DecompositionTree tree;
        AbstractStageMining miner;

        try{
            LogUtilites.addStartEndEvents(log);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to add start and end events.");
            return null;
        }

//        System.out.println("DEBUG - starting phase mining...");
        miner = new StageMiningHighestModularity();
        miner.setDebug(false);

        try{
            eTime = System.currentTimeMillis();
            tree = miner.mine(log, size);
            eTime = System.currentTimeMillis() - eTime;
//            print(tree, true);
            System.out.println("INFO - exec time: " + eTime + "ms");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to mine the stages.");
            return null;
        }

        return tree;
    }

    private void clusterize(DecompositionTree tree) {
        Set<String> activities;
        Abstraction abs;
        XLog xLog;
        SimpleLog sLog;
        XEventNameClassifier classifier = new XEventNameClassifier();
        String details;
        String sample = "";

        if( tree == null ) {
            System.out.println("ERROR - input tree null.");
            return;
        }

        try {
            clusters = new HashMap<>();
            logsHierarchy = new HashMap<>();
            for( int i=0; i <= tree.getMaxLevelIndex(); i++ ) {
                clusters.put(i, new HashMap<>());
                logsHierarchy.put(i, new HashMap<>());
                logsDetails.put(i, new HashMap<>());
                for( int j=0; j < tree.getLevel(i).size(); j++ ) {
                    details = i + "_" + j + ".xes.gz,";
                    activities = new HashSet<>();
                    for( IVertex v : tree.getLevel(i).get(j).getMemberSet() ) activities.add(sample=v.getName());
                    clusters.get(i).put(j, activities);
                    xLog = filterLog(activities);
                    sLog = LogParser.getSimpleLog(xLog, classifier);
                    abs = LogAbstraction.set(sLog);
                    details += abs.density() + ",";
                    abs = LogAbstraction.markovian(sLog,1);
                    details += abs.density() + ",";

                    if( i !=0 ) {
                        for( int k=0; k < logsHierarchy.get(i-1).size(); k++ )
                            if(clusters.get(i-1).get(k).contains(sample)) {
                                details += k;
                                break;
                            }
                    } else details += "-1";

                    logsDetails.get(i).put(j, details);
                    logsHierarchy.get(i).put(j, xLog);
                    System.out.println("INFO - parsed: " + i + "," + j);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to generate clusters.");
            return;
        }
    }

    private XLog filterLog(Set<String> activities) {
        ArrayList<String> removable = new ArrayList<>();
        XLog fxlog = cloner.cloneLog(xlog);
        HashSet<String> bridge = new HashSet<>(eventsMapping.keySet());
        bridge.removeAll(activities);
        for(String a : bridge) removable.add(eventsMapping.get(a));

        for(XTrace t : fxlog)
            for(Iterator<XEvent> iterator = t.iterator(); iterator.hasNext();) {
                XEvent e = iterator.next();
                XAttribute a = e.getAttributes().get("concept:name");
                if( a != null && removable.contains(a.toString()) ) iterator.remove();
            }

        return fxlog;
    }

    private void exportLogs(String path) {
        PrintWriter writer;

        try {
            writer = new PrintWriter(path + "\\logs_details.csv");
            writer.println("log,density-sa,density-dfg,parent-phase");
        } catch(Exception e) {
            writer = new PrintWriter(System.out);
            System.out.println("ERROR - impossible to create the file for storing the results: printing only on terminal.");
        }

        for( int i : logsHierarchy.keySet() )
            for( int j : logsHierarchy.get(i).keySet() )
                try {
                    ExportLogXesGz.export(logsHierarchy.get(i).get(j), new File(path + "\\" + i + "_" + j + ".xes.gz"));
                    writer.println(logsDetails.get(i).get(j));
                    System.out.println("DEBUG - exported: " + i + "," + j);
                } catch (IOException ioe) {
                    System.out.println("ERROR - unable to export the log: " + i + "," + j);
                    continue;
                }

        writer.flush();
        writer.close();
    }

    private void print(DecompositionTree tree, boolean check) {
        if( tree == null ) {
            System.out.println("ERROR - input tree null.");
            return;
        }

        try {
            for (int i=0; i <= tree.getMaxLevelIndex(); i++) {
                System.out.println("DEBUG - level (" + tree.getModularity(i) + "): " + i);
                for (int j=0; j < tree.getLevel(i).size(); j++) {
                    System.out.println("\t phase: " + j + " - transition node: " + tree.getLevel(i).get(j).getSink().getName());
                    for( IVertex v : tree.getLevel(i).get(j).getMemberSet() ) {
                        if( check && !eventsMapping.containsKey(v.getName()) ) System.out.println("WARNING - activity not found(!):" + v.getName());
                        System.out.println("\t\t" + v.getName());
                    }
//                    System.out.println("Cut-set: " + ((Vertex2)tree.getLevel(i).get(j).getSink()).getMinCutSet());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to explore and print the decomposition tree.");
            return;
        }
    }
}
