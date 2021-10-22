package au.edu.unimelb.processmining.compliance;

import com.raffaeleconforti.log.util.LogImporter;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class TimeConstraintsChecker implements Serializable {

    public enum QCODE {MAX, MIN, MAXNOS, MINNOS, EXACT}

    private Map<Integer, Set<Integer>> caseObservations; //key is the activityID, value is the set of caseIDs where that activity is observed
    private Map<Pair, TreeSet<Pair>> ascendingDeltas; //key pair is a pair source-target, array pair is a pair delta-caseID, in ascending order by delta
    private Map<String, Integer> activityIDs;
    private Map<Integer, String> caseIDs;

    private int aID; //counter used to map the activities to numbers
    long eventcounter;
    long paircounter;
    private boolean loaded;

    public TimeConstraintsChecker() {
        aID = 1;
        ascendingDeltas = new HashMap<>();
        caseObservations = new HashMap<>();
        activityIDs = new HashMap<>();
        caseIDs = new HashMap<>();
        loaded = false;
    }

    public boolean readXLog(String logPath) {
        XLog log = null;
        XEventClassifier xEventClassifier = new XEventNameClassifier();

        int caseID; //index to iterate on the log traces
        int eIndex; //index to iterate on the events of the trace
        int aIndex; //index to work on activities IDs

        Pair pair;
        long delta;

        XTrace trace;
        int traceSize;
        int totalTraces;

        XEvent event;
        String label;

        ArrayList<Integer> activities;
        ArrayList<Long> timestamps;

        if(loaded) {
            aID = 1;
            ascendingDeltas = new HashMap<>();
            caseObservations = new HashMap<>();
            activityIDs = new HashMap<>();
            caseIDs = new HashMap<>();
            loaded = false;
            paircounter = 0;
            eventcounter = 0;
        }

        try {
            log = LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath);
        } catch(Exception e) {
            System.out.println("ERROR - impossible to load the log");
            e.printStackTrace();
            return false;
        }

        loaded = true;
        totalTraces = log.size();
        for( caseID = 0; caseID < totalTraces; caseID++ ) {
//            we have two arrays, one stores the observed activities, one stores their times
            activities = new ArrayList<>();
            timestamps = new ArrayList<>();
            activities.add(0); // this is necessary to assume a standard start event

            trace = log.get(caseID);
            caseIDs.put(caseID, trace.getAttributes().get("concept:name").toString());
            traceSize = trace.size();

//            for each trace, we iterate on all the event. this iteration is equal to O(e), with e = total number of events
            for( eIndex = 0; eIndex < traceSize; eIndex++ ) {
                eventcounter++;
                event = trace.get(eIndex);
                label = xEventClassifier.getClassIdentity(event);
                if(!activityIDs.containsKey(label)) {
                    aIndex = aID;
                    activityIDs.put(label, aID++);
                    caseObservations.put(aIndex, new HashSet<>());
                } else aIndex = activityIDs.get(label);
                caseObservations.get(aIndex).add(caseID);
                activities.add(aIndex);
                timestamps.add(getTimestampInSeconds(event.getAttributes().get("time:timestamp").toString()));
                if(eIndex == 0) timestamps.add(timestamps.get(0)); // we duplicate the first timestamp to calculate delta w.r.t. the start
            }

//            this is a double loop to calculate the delta-t between pair activities, this has a complexity of O(n*(n+1)/2) = O(n^2). n = e
            int n = activities.size();
            for(int i = 0; i < n-1; i++)
                for(int j = i+1; j < n; j++) {
                    pair = new Pair(activities.get(i), activities.get(j));
                    delta = timestamps.get(j) - timestamps.get(i);
                    if(!ascendingDeltas.containsKey(pair)) {
                        ascendingDeltas.put(pair, new TreeSet<>());
                    }
                    ascendingDeltas.get(pair).add(new Pair(delta,caseID));
                    paircounter++;
                }
        }

        System.out.println("DEBUG - events: " + eventcounter);
        System.out.println("DEBUG - total activities: " + activityIDs.size());
        System.out.println("DEBUG - total distinct pairs: " + ascendingDeltas.size());
        System.out.println("DEBUG - total pairs: " + paircounter);

        return loaded;
    }

    public void print(){
        if(!loaded) {
            System.out.println("ERROR - no log available");
            return;
        }

        for(Pair p1 : ascendingDeltas.keySet()) {
            System.out.println(p1);
            System.out.print(": ");
            for(Pair p2 : ascendingDeltas.get(p1)) System.out.print(p2 + " : ");
            System.out.println();
        }
    }

    private long getTimestampInSeconds(String date) {
        return ((new GregorianCalendar(
                Integer.valueOf(date.substring(0,4)),
                Integer.valueOf(date.substring(5,7)),
                Integer.valueOf(date.substring(8,10)),
                Integer.valueOf(date.substring(11,13)),
                Integer.valueOf(date.substring(14,16)),
                Integer.valueOf(date.substring(17,19)))).getTimeInMillis())/1000;
    }

    public void checkConstraints(String path) {
        if(!loaded) return;

        Set<Integer> invalidCases;

        BufferedReader reader;
        String query;
        int source;
        int target;
        QCODE code;
        long delta;
        StringTokenizer tokenizer;

        try {
            reader = new BufferedReader(new FileReader(path));

            while (reader.ready()) {
                query = reader.readLine();
                tokenizer = new StringTokenizer(query, " ");
                source = Integer.valueOf(tokenizer.nextToken()); // replace this with a get from activitiesID
                target = Integer.valueOf(tokenizer.nextToken()); // replace this with a get from activitiesID
                code = QCODE.valueOf(tokenizer.nextToken());
                delta = Long.valueOf(tokenizer.nextToken());
                invalidCases = query(source, target, code, delta);
                System.out.println("RESULT (" + invalidCases.size() + ")- query: " + query);
//                for(int caseID : invalidCases) System.out.println("DEBUG - bad case: " + caseID);
            }

            reader.close();
        } catch ( IOException ioe ) {
            System.out.println("ERROR - something went wrong while reading the file: " + path);
            return;
        }
    }


    public Set<Integer> query (int source, int target, QCODE query, long delta) {
        if(!loaded) return null;
        Set<Integer> invalidCases = new HashSet<>();
        boolean inclusive = true;

        Pair p;
        Pair ref;

        switch(query){
            case MAX:
                p = new Pair(source, target);
                ref = new Pair(delta, Long.MIN_VALUE);
                for(Pair x : ascendingDeltas.get(p).tailSet(ref, inclusive)) invalidCases.add(x.getRight());
                break;
            case MIN:
                p = new Pair(source, target);
                ref = new Pair(delta, Long.MAX_VALUE);
                for(Pair x : ascendingDeltas.get(p).headSet(ref, inclusive)) invalidCases.add(x.getRight());
                break;
            case EXACT:
                p = new Pair(source, target);
                for(Pair x : ascendingDeltas.get(p).subSet(new Pair(delta, Long.MIN_VALUE), true, new Pair(delta, Long.MIN_VALUE), true)) invalidCases.add(x.getRight());
                break;
            case MAXNOS:
                p = new Pair(0, target);
                ref = new Pair(delta, Long.MIN_VALUE);
                if(ascendingDeltas.get(p) == null) break;
                for(Pair x : ascendingDeltas.get(p).tailSet(ref, inclusive)) invalidCases.add(x.getRight());
                invalidCases.removeAll(caseObservations.get(source));
                break;
            case MINNOS:
                p = new Pair(0, target);
                ref = new Pair(delta, Long.MAX_VALUE);
                if(ascendingDeltas.get(p) == null) break;
                for(Pair x : ascendingDeltas.get(p).headSet(ref, inclusive)) invalidCases.add(x.getRight());
                invalidCases.removeAll(caseObservations.get(source));
                break;
            default:
                System.out.println("ERROR - invalid query code");
                break;
        }

        return invalidCases;
    }

}


class Pair implements Comparable {
    long left;
    long right;
    String string;

    Pair(long left, long right) {
        this.left = left;
        this.right = right;
        this.string = "(" + left + "," + right + ")";
    }

    long getLeft() {return left;}
    int getRight() {return (int) right;}

    @Override
    public int compareTo(Object o) {
        if(o instanceof Pair) {
            if(this.left == ((Pair)o).left) return (int) (this.right - ((Pair)o).right);
            else return (int) (this.left - ((Pair)o).left);
        } else return -1;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Pair) return (((Pair)o).left == this.left && ((Pair)o).right == this.right);
        else return false;
    }

    @Override
    public String toString(){
        return string;
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

}
