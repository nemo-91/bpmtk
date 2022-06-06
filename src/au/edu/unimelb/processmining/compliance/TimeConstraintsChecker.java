package au.edu.unimelb.processmining.compliance;

import com.raffaeleconforti.log.util.LogImporter;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.*;
import java.util.*;

public class TimeConstraintsChecker implements Serializable {

    public enum QCODE {MAXP, MINP, EXACTP, MAX, MIN, EXACT, MAXS, MINS, EXACTS}

    private Map<Long, Set<Long>> caseObservations; //key is the activityID, value is the set of caseIDs where that activity is observed
    private Map<Pair, TreeSet<Pair>> ascendingDeltas; //key pair is a pair source-target, array pair is a pair delta-caseID, in ascending order by delta
    private Map<String, Long> activityIDs;
//    private Map<Long, String> labelIDs;
    private Map<String, Long> caseIDs;

    private Map<String, ArrayList<Long>> logActivites;
    private Map<String, ArrayList<Long>> logTimestamps;

    private long aID; //counter used to map the activities to numbers
    private long caseID; //counter used to map the caseIDs to their name/id
    private boolean loaded;

    private static boolean ABT = true; // flag that enables the handling of broken traces across different log fragments

    public TimeConstraintsChecker() {
        ascendingDeltas = new HashMap<>();
        caseObservations = new HashMap<>();
        activityIDs = new HashMap<>();
//        labelIDs = new HashMap<>();
        caseIDs = new HashMap<>();
        logActivites = new HashMap<>();
        logTimestamps = new HashMap<>();

        aID = 1;
        caseID = 0;
        loaded = false;
    }

    public boolean readXLog(String logPath) {
        XLog log = null;
        long negTimes = 0;
        long paircounter = 0;
        long eventcounter = 0;
        long brokenTraces = 0;
        XEventClassifier xEventClassifier = new XEventNameClassifier();

        long aIndex; //index to work on activity IDs
        long cIndex; //index to work on case IDs

        Pair pair;
        long delta;

        XTrace trace;
        int traceSize;
        int maxTraceSize = 0;
        int totalTraces;
        int traceIndex; //index to iterate on the log traces
        int eIndex; //index to iterate on the events of the trace

        XEvent event;
        String label;
        String caseStringID;

        ArrayList<Long> activities;
        ArrayList<Long> timestamps;

        long tmpTS;
        boolean duplicate;

        try {
            log = LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath);
        } catch(Exception e) {
            System.out.println("ERROR - no log given in input");
            e.printStackTrace();
            return false;
        }

        if(log == null) {
            System.out.println("ERROR - no log given in input");
            return false;
        }

        long etime = System.currentTimeMillis();

        totalTraces = log.size();
        for( traceIndex = 0; traceIndex < totalTraces; traceIndex++ ) {
            trace = log.get(traceIndex);
            traceSize = trace.size();
            caseStringID = trace.getAttributes().get("concept:name").toString();
            if(!caseIDs.containsKey(caseStringID)) caseIDs.put(caseStringID, caseID++);
            cIndex = caseIDs.get(caseStringID);

//            we have two arrays, one stores the observed activities, one stores their times
            if(ABT && logActivites.containsKey(caseStringID)) {
                activities = logActivites.get(caseStringID);
                timestamps = logTimestamps.get(caseStringID);
                brokenTraces++;
            } else {
                timestamps = new ArrayList<>();
                activities = new ArrayList<>();
                activities.add((long) 0); // this is necessary to assume a standard start event
            }

//            for each trace, we iterate on all the event. this iteration is equal to O(e), with e = total number of events
            for( eIndex = 0; eIndex < traceSize; eIndex++ ) {
                eventcounter++;
                event = trace.get(eIndex);
                label = xEventClassifier.getClassIdentity(event);
                if(!activityIDs.containsKey(label)) {
                    aIndex = aID;
                    activityIDs.put(label, aID++);
//                    labelIDs.put(aIndex, label);
                    caseObservations.put(aIndex, new HashSet<>());
                } else aIndex = activityIDs.get(label);

                tmpTS = getTimestampInMinutes(event.getAttributes().get("time:timestamp").toString());
                if(activities.size() == 1) timestamps.add(tmpTS); // we duplicate the first timestamp to calculate delta w.r.t. the start

                duplicate = false;
                for(int j = activities.size() - 1; j >= 0 ; j--) {
//                    the following IF allows to keep track which one is the last occurrence of an activity (which will always be a positive number)
//                    past occurrences of activities will be negative numbers
                    if(activities.get(j) == aIndex) {
                        activities.set(j, -activities.get(j));
                        duplicate = true;
                    }

/*
                    pairs (positive, positive) are two eventually-follows activities with no in-between duplicates of the source or target
                    pairs (positive, negative) are two eventually-follows activities with no in-between duplicates of the source but duplicates of the target
                    pairs (negative, positive) are two eventually-follows activities with no in-between duplicates of the target but duplicates of the source
                    pairs (negative, negative) are two eventually-follows activities with in-between duplicates of the target and the source
 */

                    if(duplicate) pair = new Pair(activities.get(j), -aIndex);
                    else pair = new Pair(activities.get(j), aIndex);

                    delta = tmpTS - timestamps.get(j);
                    if(delta < 0) negTimes++;
                    if(!ascendingDeltas.containsKey(pair)) ascendingDeltas.put(pair, new TreeSet<>());
                    ascendingDeltas.get(pair).add(new Pair(delta, cIndex));
                    paircounter++;
                }

                if(maxTraceSize < traceSize) maxTraceSize = traceSize;
                activities.add(aIndex);
                timestamps.add(tmpTS);
            }

            if(ABT) {
                logActivites.put(caseStringID, activities);
                logTimestamps.put(caseStringID, timestamps);
            }
        }

        System.out.println("DEBUG - traces: " + totalTraces);
        System.out.println("DEBUG - trace max length: " + maxTraceSize);
        System.out.println("DEBUG - events: " + eventcounter);
        System.out.println("DEBUG - total activities: " + activityIDs.size());
        System.out.println("DEBUG - total distinct pairs: " + ascendingDeltas.size());
        System.out.println("DEBUG - total pairs: " + paircounter);
//        System.out.println("DEBUG - negative times: " + negTimes);
        System.out.println("DEBUG - broken traces: " + brokenTraces);
        
        etime = System.currentTimeMillis() - etime;
        System.out.println("Reading TIME - " + (double)etime/1000.0 + "s");

        return (loaded = true);
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

    private long getTimestampInMinutes(String date) {
        return ((new GregorianCalendar(
                Integer.valueOf(date.substring(0,4)),
                Integer.valueOf(date.substring(5,7)),
                Integer.valueOf(date.substring(8,10)),
                Integer.valueOf(date.substring(11,13)),
                Integer.valueOf(date.substring(14,16)),
                Integer.valueOf(date.substring(17,19)))).getTimeInMillis())/60000;
    }

    private long getTimestampInHours(String date) {
        return ((new GregorianCalendar(
                Integer.valueOf(date.substring(0,4)),
                Integer.valueOf(date.substring(5,7)),
                Integer.valueOf(date.substring(8,10)),
                Integer.valueOf(date.substring(11,13)),
                Integer.valueOf(date.substring(14,16)),
                Integer.valueOf(date.substring(17,19)))).getTimeInMillis())/3600000;
    }

    public void info() {
        long max, min;
        for(Pair x : ascendingDeltas.keySet()) {
            min = ascendingDeltas.get(x).first().left;
            max = ascendingDeltas.get(x).last().left;
            System.out.println(x.left + "," + x.right + "," + min + "," + max);
        }
    }

    public void checkConstraints(String path) {
        if(!loaded) return;

        Set<Long> invalidCases;

        BufferedReader reader;
        String query = null;
        String source;
        String target;
        QCODE code;
        long delta;
        StringTokenizer tokenizer;

        int totalInvalidCases = 0;
        int counter = 0;

        long eTime;

        boolean allInstances = false;

        try {
            reader = new BufferedReader(new FileReader(path));

            while (reader.ready()) {
                query = reader.readLine();
                System.out.println("QUERY - " + query);
                eTime = System.currentTimeMillis();
                if(query.charAt(0) == '*') {
//                    System.out.println("DEBUG - requesting all instances");
                    query = query.substring(1);
                    allInstances = true;
                }
                tokenizer = new StringTokenizer(query, "::");
                source = tokenizer.nextToken();
                target = tokenizer.nextToken();
                code = QCODE.valueOf(tokenizer.nextToken());
                delta = Long.valueOf(tokenizer.nextToken());
                invalidCases = query(source, target, code, delta);
                if(allInstances) {
                    invalidCases.addAll(query(source, '-' + target, code, delta));
                    invalidCases.addAll(query('-' + source, target, code, delta));
                    invalidCases.addAll(query('-' + source, '-'+target, code, delta));
                }
                allInstances = false;
                System.out.println("HITS - " + invalidCases.size());
                totalInvalidCases += invalidCases.size();
                counter++;
//                System.out.println("RESULT (" + invalidCases.size() + ")- query: " + query);
//                for(int caseID : invalidCases) System.out.println("DEBUG - bad case: " + caseID);
                System.out.println("TIME - " + (System.currentTimeMillis() - eTime));
            }

            if(counter !=0) System.out.println("DEBUG - average hits ("+ totalInvalidCases +"): " + totalInvalidCases/counter);
            reader.close();
        } catch ( IOException ioe ) {
            System.out.println("ERROR - something went wrong while reading the file: " + path);
            return;
        } catch ( Exception e ) {
            System.out.println("ERROR - something went wrong while checking this rule: " + query);
            return;
        }
    }


    public Set<Long> query (String src, String tgt, QCODE query, long delta) {
        if(!loaded) return null;
        Set<Long> invalidCases = new HashSet<>();
        Map<Long, Long> tmp = new HashMap();
//        boolean inclusive = false; this is useless at the moment - the delta is not inclusive
        boolean strictFirst = true;

        Pair p;
        Pair ref;

        Long source;
        Long target;

        try {
//            this is effective only if the activity is given as integer ID
//            this is mostly required for efficient testing
            source = Long.valueOf(src);
            target = Long.valueOf(tgt);
//            System.out.println("-" + source + " -" + target + " " + query + " " + delta);
//            System.out.println("\"*" + labelIDs.get(source) + "\" \"*" + labelIDs.get(target) + "\" " + query + " " + delta);
        } catch(Exception e) {
//            if the activity is not given as an integerID, we need to look it up
            if(src.charAt(0) == '-') source = -activityIDs.get(src.substring(1));
            else source = activityIDs.get(src);

            if(tgt.charAt(0) == '-') target = -activityIDs.get(tgt.substring(1));
            else target = activityIDs.get(tgt);
        }

        if(source == null || target == null) return invalidCases;

        switch(query){
            case MAX:
//                cases where if TGT follows SRC, it must follow within DELTA-time
//                it returns the cases where: the first occurrence of TGT after SRC is observed after DELTA-time
                invalidCases.addAll(caseObservations.get(Math.abs(source)));
                invalidCases.removeAll(caseObservations.get(Math.abs(target)));
                p = new Pair(source, target);
                if(ascendingDeltas.get(p) == null) break;
                ref = new Pair(delta, Long.MIN_VALUE);
                for(Pair x : ascendingDeltas.get(p).tailSet(ref, false)) invalidCases.add(x.right);
                break;
            case MIN:
//                cases where if TGT follows SRC, it must follow after DELTA-time
//                it returns the cases where: the first occurrence of TGT after SRC is observed before DELTA-time
                p = new Pair(source, target);
                if(ascendingDeltas.get(p) == null) break;
                ref = new Pair(delta, Long.MAX_VALUE);
                for(Pair x : ascendingDeltas.get(p).headSet(ref, false)) invalidCases.add(x.right);
                break;
            case EXACT:
//                cases where if TGT follows SRC, it must follow exactly after DELTA-time
//                it returns the cases where: the first occurrence of TGT after SRC is observed exactly at DELTA-time
                p = new Pair(source, target);
                if(ascendingDeltas.get(p) == null) break;
                ref = new Pair(delta, Long.MAX_VALUE);
                for(Pair x : ascendingDeltas.get(p).tailSet(ref, false)) invalidCases.add(x.right);
                ref = new Pair(delta, Long.MIN_VALUE);
                for(Pair x : ascendingDeltas.get(p).headSet(ref, false)) invalidCases.add(x.right);
                break;
            case MAXS:
//                cases where if SRC is not observed, TGT must be observed within DELTA-time
//                it returns the cases where: the first occurrence of TGT is observed after DELTA-time from start && SRC is not observed
                invalidCases.addAll(caseObservations.get(Math.abs(source)));
                invalidCases.removeAll(caseObservations.get(Math.abs(target)));
                p = new Pair(0, target);
                if(ascendingDeltas.get(p) == null) break;
                ref = new Pair(delta, Long.MIN_VALUE);
//                this considers all the observation of TGT after the start not only the first one!
                for(Pair x : ascendingDeltas.get(p).tailSet(ref, false)) invalidCases.add(x.right);
                invalidCases.removeAll(caseObservations.get(Math.abs(source)));
                break;
            case MINS:
//                cases where if SRC is not observed, TGT must be observed after DELTA-time
//                it returns the cases where: the first occurrence of TGT is observed within DELTA-time from start && SRC is not observed
                p = new Pair(0, target);
                if(ascendingDeltas.get(p) == null) break;
                ref = new Pair(delta, Long.MAX_VALUE);
//                this may consider all the observation of TGT after the start, but it is okay because if there is one that brakes the rule also all the previous break the rule
//                e.g., in the best case only the first breaks the rule, in the worst case, the last and all the preceding (till the first observation) break the rule
                for(Pair x : ascendingDeltas.get(p).headSet(ref, false)) invalidCases.add(x.right);
                invalidCases.removeAll(caseObservations.get(Math.abs(source)));
                break;
            case EXACTS:
//                cases where if SRC is not observed, TGT must be observed exactly at DELTA-time
//                it returns the cases where: the first occurrence of TGT is observed exactly at DELTA-time from start && SRC is not observed
                p = new Pair(0, target);
                invalidCases.addAll(caseIDs.values());
                if(ascendingDeltas.get(p) == null) break;
//                ref = new Pair(delta, Long.MIN_VALUE);
                for(Pair x : ascendingDeltas.get(p).subSet(new Pair(delta, Long.MIN_VALUE), new Pair(delta, Long.MAX_VALUE))) invalidCases.remove(x.right);
                ref = new Pair(delta, Long.MIN_VALUE);
//                iterating on the cases where TGT is observed before DELTA-time
                for(Pair x : ascendingDeltas.get(p).headSet(ref, false)) invalidCases.add(x.right);
                invalidCases.removeAll(caseObservations.get(Math.abs(source)));
                break;
            case MAXP:
//                cases where TGT MUST follow SRC (if it appears) within DELTA-time
//                cases where SRC does not precede TGT are considered invalid (this accounts only for the first observation)
//                it returns the cases where: the first occurrence of TGT follows SRC after DELTA-time && where SRC does not precede TGT

                p = new Pair(source, target);
                if(ascendingDeltas.get(p) == null) break;
                ref = new Pair(delta, Long.MIN_VALUE);
                for(Pair x : ascendingDeltas.get(p).tailSet(ref, false)) invalidCases.add(x.right);

                p = new Pair(0, source);
                for(Pair x : ascendingDeltas.get(p))
                    if(!tmp.containsKey(x.right)) tmp.put(x.right, x.left);

                p = new Pair(0, target);
                for(Pair x : ascendingDeltas.get(p))
                    if(!tmp.containsKey(x.right) || tmp.get(x.right) >= x.left ) invalidCases.add(x.right);
                break;
            case MINP:
//                cases where TGT MUST follow SRC (if it appears) after DELTA-time
//                cases where SRC does not precede TGT are considered invalid (this accounts only for the first observation)
//                it returns the cases where: the first occurrence of TGT follows SRC within DELTA-time and where SRC does not precede TGT
                p = new Pair(source, target);
                if(ascendingDeltas.get(p) == null) break;
                ref = new Pair(delta, Long.MAX_VALUE);
                for(Pair x : ascendingDeltas.get(p).headSet(ref, false)) invalidCases.add(x.right);

                p = new Pair(0, source);
                for(Pair x : ascendingDeltas.get(p))
                    if(!tmp.containsKey(x.right)) tmp.put(x.right, x.left);

                p = new Pair(0, target);
                for(Pair x : ascendingDeltas.get(p))
                    if(!tmp.containsKey(x.right) || tmp.get(x.right) <= x.left ) invalidCases.add(x.right);
                break;
            case EXACTP:
//                cases where TGT MUST follow SRC (if it appears) exactly at DELTA-time
//                cases where SRC does not precede TGT are considered invalid (this accounts only for the first observation)
//                it returns the cases where: the first occurrence of TGT follows SRC within DELTA-time and where SRC does not precede TGT
                p = new Pair(source, target);
                if(ascendingDeltas.get(p) == null) break;
                ref = new Pair(delta, Long.MAX_VALUE);
                for(Pair x : ascendingDeltas.get(p).tailSet(ref, false)) invalidCases.add(x.right);
                ref = new Pair(delta, Long.MIN_VALUE);
                for(Pair x : ascendingDeltas.get(p).headSet(ref, false)) invalidCases.add(x.right);

                p = new Pair(0, source);
                for(Pair x : ascendingDeltas.get(p))
                    if(!tmp.containsKey(x.right)) tmp.put(x.right, x.left);

                p = new Pair(0, target);
                for(Pair x : ascendingDeltas.get(p))
                    if(!tmp.containsKey(x.right) || tmp.get(x.right) >= x.left ) invalidCases.add(x.right);
                break;
            default:
                System.out.println("ERROR - invalid query code");
                break;
        }

        return invalidCases;
    }


    public boolean saveData(String path) {
        PrintWriter writer;

        System.out.print("DEBUG - saving the data... ");
        long etime = System.currentTimeMillis();

        try {
            writer = new PrintWriter(".\\out.tcc");

            writer.println(caseID);
            writer.println(aID);

            writer.print("AIDs");
            for(String a : activityIDs.keySet()) writer.print("," + a + "," + activityIDs.get(a));
            writer.println();

            writer.print("CIDs");
            for(String c : caseIDs.keySet()) writer.print("," + c + "," + caseIDs.get(c));
            writer.println();

            if(ABT) {
//                how many row iterations will be required to print the "log" in reduced form
                writer.println(logActivites.size());

                for(String caseStringID : logActivites.keySet()) {
                    writer.print(caseStringID);
                    for (int i = 0; i < logActivites.get(caseStringID).size(); i++)
                        writer.print("," + logActivites.get(caseStringID).get(i) + "," + logTimestamps.get(caseStringID).get(i));
                    writer.println();
                }
            }

            for(long aid : caseObservations.keySet()) {
                writer.print(aid);
                for (long cid : caseObservations.get(aid)) writer.print("," + cid);
                writer.println();
            }

            for(Pair p1 : ascendingDeltas.keySet()) {
                writer.print(p1.left + "," + p1.right);
                for (Pair p2 : ascendingDeltas.get(p1)) writer.print("," + p2.left + "," + p2.right);
                writer.println();
            }

            writer.close();
            System.out.println("done in " + (double)(System.currentTimeMillis() - etime)/1000.0 + "s");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loadData(String path) {
        if(loaded || !path.endsWith(".tcc")) return false;

        StringTokenizer tokenizer;
        long aid;
        Pair pair;
        HashSet<Long> cids;
        TreeSet<Pair> tree;

        long cases;
        String caseStringID;

        ArrayList<Long> tmpActivities;
        ArrayList<Long> tmpTimestamps;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            System.out.print("DEBUG - loading the data... ");
            long etime = System.currentTimeMillis();

//            the first line is the caseID
            caseID = Long.valueOf(reader.readLine());
//            the second line is the activityID
            aID = Long.valueOf(reader.readLine());

//            the third line contains the mapping of the activity IDs
            tokenizer = new StringTokenizer(reader.readLine(), ",");
            tokenizer.nextToken(); // we burn the first token because it is a standard label
            while(tokenizer.hasMoreTokens())
                activityIDs.put(tokenizer.nextToken(), Long.valueOf(tokenizer.nextToken()));

//            the fourth line contains the mapping of the case IDs
            tokenizer = new StringTokenizer(reader.readLine(), ",");
            tokenizer.nextToken(); // we burn the first token because it is a standard label
            while(tokenizer.hasMoreTokens())
                caseIDs.put(tokenizer.nextToken(), Long.valueOf(tokenizer.nextToken()));

            if(ABT) {
//                how many row iterations will be required to print the "log" in reduced form
                cases = Integer.valueOf(reader.readLine());
                logActivites = new HashMap<>();
                logTimestamps = new HashMap<>();

                for(int i = 0; i < cases; i++) {
                    tokenizer = new StringTokenizer(reader.readLine(), ",");
                    caseStringID = tokenizer.nextToken();
                    tmpActivities = new ArrayList<>();
                    tmpTimestamps = new ArrayList<>();
                    while(tokenizer.hasMoreTokens()) {
                        tmpActivities.add(Long.valueOf(tokenizer.nextToken()));
                        tmpTimestamps.add(Long.valueOf(tokenizer.nextToken()));
                    }
                    logActivites.put(caseStringID, tmpActivities);
                    logTimestamps.put(caseStringID, tmpTimestamps);
                }
            }

//            the next X(=aID) lines contain the list of cases where we observed each activity
            for(long i = 1; i < aID; i++) {
                cids = new HashSet<>();
                tokenizer = new StringTokenizer(reader.readLine(), ",");
                aid = Long.valueOf(tokenizer.nextToken());
                while(tokenizer.hasMoreTokens()) cids.add(Long.valueOf(tokenizer.nextToken()));
                caseObservations.put(aid, cids);
            }

//            the remaining lines contain the list of deltas per pairs
            while (reader.ready())  {
                tree = new TreeSet<>();
                tokenizer = new StringTokenizer(reader.readLine(), ",");
                pair = new Pair(Long.valueOf(tokenizer.nextToken()), Long.valueOf(tokenizer.nextToken()));
                while(tokenizer.hasMoreTokens()) tree.add(new Pair(Long.valueOf(tokenizer.nextToken()), Long.valueOf(tokenizer.nextToken())));
                ascendingDeltas.put(pair, tree);
            }

            reader.close();
            System.out.println("done in " +  (double)(System.currentTimeMillis() - etime)/1000.0 + "s");
            return (loaded = true);
        } catch (Exception e) {
            System.out.println("ERROR - something went wrong loading the data from: " + path);
            return false;
        }
    }

}


class Pair implements Comparable, Serializable {
    Long left;
    Long right;
    String string;
    int hashcode;

    Pair(long left, long right) {
        this.left = left;
        this.right = right;
        this.string = "(" + left + "," + right + ")";
        hashcode = this.string.hashCode();
    }

    long getLeft() {return left;}
    long getRight() {return right;}

    @Override
    public int compareTo(Object o) {
        if(o instanceof Pair) {
            if(this.left == ((Pair)o).left) return this.right.compareTo(((Pair)o).right);
            else return  this.left.compareTo(((Pair)o).left);
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
        return hashcode;
    }

}
