/*
 * Copyright © 2009-2018 The Apromore Initiative.
 *
 * This file is part of "Apromore".
 *
 * "Apromore" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * "Apromore" is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package au.edu.qut.processmining.log;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Adriano on 14/06/2016.
 */
public class LogParser {

//    !!WARNING - DO NOT CHANGE THESE VALUES!
    public static final int STARTCODE = 0;
    public static final int ENDCODE = -1;


    public static SimpleLog getSimpleLog(String path) {
        SimpleLog log;

        HashSet<String> labels = new HashSet<>();
        ArrayList<String> orderedLabels;
        HashMap<String, Integer> labelsToIDs = new HashMap<>();  //this maps the original name of an event to its code
        HashMap<Integer, String> events = new HashMap<>();  //this maps the code of the event to its original name
        HashMap<String, Integer> reverseMap = new HashMap<>();  //this maps the event name to its code
        HashMap<String, Integer> traces = new HashMap<>();  //this is the simple log, each trace is a string associated to its frequency

        int frequency;
        String trace;
        String strace;
        String event;
        StringTokenizer tokenizer;

        int LID;

        BufferedReader reader;

        events.put(STARTCODE, "autogen-start");
        events.put(ENDCODE, "autogen-end");

        try {
            reader = new BufferedReader(new FileReader(path));

            while( reader.ready() )
            {
                trace = reader.readLine();
                tokenizer = new StringTokenizer(trace, "::");
                tokenizer.nextToken();

                while( tokenizer.hasMoreTokens() ) {
                    event = tokenizer.nextToken();
                    labels.add(event);
                }
            }

            reader.close();

            orderedLabels = new ArrayList<>(labels);
            Collections.sort(orderedLabels);

            LID = 1;
            for (String l : orderedLabels) {
                labelsToIDs.put(l, LID);
                events.put(LID, l);
                reverseMap.put(l, LID);
                LID++;
            }

            reader = new BufferedReader(new FileReader(path));

            while( reader.ready() )
            {
                trace = reader.readLine();
                tokenizer = new StringTokenizer(trace, "::");
                frequency = Integer.valueOf(tokenizer.nextToken());

                strace = "::" + STARTCODE + "::";
                while( tokenizer.hasMoreTokens() ) {
                    event = tokenizer.nextToken();
                    strace += (labelsToIDs.get(event) + "::");
                }
                strace += ENDCODE + "::";

                if(!traces.containsKey(strace))  traces.put(strace, frequency);
                else traces.put(strace, traces.get(strace) + frequency);
            }

            reader.close();

            log = new SimpleLog(traces, events, null);
            log.setReverseMap(reverseMap);
            log.setStartcode(STARTCODE);
            log.setEndcode(ENDCODE);

        } catch ( IOException ioe ) {
            System.out.println("ERROR - something went wrong while reading the log file: " + path);
            return null;
        }

        return log;

    }

    public static SimpleLog getSimpleLog(XLog log, XEventClassifier xEventClassifier) {
//        System.out.println("LOGP - starting ... ");
//        System.out.println("LOGP - input log size: " + log.size());

        SimpleLog sLog;

        HashSet<String> labels = new HashSet<>();
        ArrayList<String> orderedLabels;
        HashMap<String, Integer> labelsToIDs = new HashMap<>();  //this maps the original name of an event to its code
        HashMap<Integer, String> events = new HashMap<>();  //this maps the code of the event to its original name
        HashMap<String, Integer> reverseMap = new HashMap<>();  //this maps the event name to its code
        HashMap<String, Integer> traces = new HashMap<>();  //this is the simple log, each trace is a string associated to its frequency

        int tIndex; //index to iterate on the log traces
        int eIndex; //index to iterate on the events of the trace

        XTrace trace;
        String sTrace;

        XEvent event;
        String label;

        int LID;
        long totalEvents;
        long oldTotalEvents;

        long traceLength;
        long longestTrace = Integer.MIN_VALUE;
        long shortestTrace = Integer.MAX_VALUE;

        int totalTraces = log.size();
        long traceSize;

        int[] exclusiveness;
        Set<Integer> executed = new HashSet<>();

        events.put(STARTCODE, "autogen-start");
        events.put(ENDCODE, "autogen-end");


        for( tIndex = 0; tIndex < totalTraces; tIndex++ ) {
            /*  we firstly get all the concept names
            *   and we map them into numbers for fast processing
            */

            trace = log.get(tIndex);
            traceSize = trace.size();

            for( eIndex = 0; eIndex < traceSize; eIndex++ ) {
                event = trace.get(eIndex);
                label = xEventClassifier.getClassIdentity(event);
                labels.add(label);
            }
        }

        orderedLabels = new ArrayList<>(labels);
        Collections.sort(orderedLabels);

        LID = 1;
        for( String l : orderedLabels ) {
            labelsToIDs.put(l, LID);
            events.put(LID, l);
            reverseMap.put(l, LID);
//            System.out.println("DEBUG - ID:label - " + LID + ":" + l);
            LID++;
        }

        exclusiveness = new int[LID*LID];
        for(int i = 0; i<exclusiveness.length; i++) exclusiveness[i] = 0;

        totalEvents = 0;
        for( tIndex = 0; tIndex < totalTraces; tIndex++ ) {
            executed.clear();
            /* we convert each trace in the log into a string
            *  each string will be a sequence of "::x" terminated with "::", where:
            *  '::' is a separator
            *  'x' is an integer encoding the name of the original event
            */
            trace = log.get(tIndex);
            traceSize = trace.size();

            oldTotalEvents = totalEvents;
            sTrace = "::" + Integer.toString(STARTCODE) + ":";
            for( eIndex = 0; eIndex < traceSize; eIndex++ ) {
                totalEvents++;
                event = trace.get(eIndex);
                label = xEventClassifier.getClassIdentity(event);
                sTrace += ":" + labelsToIDs.get(label).toString() + ":";
                executed.add(labelsToIDs.get(label));
            }
            sTrace += ":" + Integer.toString(ENDCODE) + "::";

            traceLength = totalEvents - oldTotalEvents;
            if( longestTrace < traceLength ) longestTrace = traceLength;
            if( shortestTrace > traceLength ) shortestTrace = traceLength;

            if( !traces.containsKey(sTrace) ) traces.put(sTrace, 0);
            traces.put(sTrace, traces.get(sTrace)+1);

            for(int a=0; a < LID; a++) {
                if(!executed.contains(a)) {
                    for(int x : executed) {
                        exclusiveness[x*LID + a]++;
                        exclusiveness[a*LID + x]++;
                    }
                }
            }
        }

//        System.out.println("LOGP - total events parsed: " + totalEvents);
//        System.out.println("LOGP - total distinct events: " + (events.size() - 2) );
//        System.out.println("LOGP - total distinct traces: " + traces.size() );

//        for( String t : traces.keySet() ) System.out.println("DEBUG - ["+ traces.get(t) +"] trace: " + t);

//        System.out.println("DEBUG - final mapping:");
//        for( int code : events.keySet() ) System.out.println("DEBUG - " + code + " = " + events.get(code));

        sLog = new SimpleLog(traces, events, log);
        sLog.setExclusiveness(exclusiveness);
        sLog.setReverseMap(reverseMap);
        sLog.setStartcode(STARTCODE);
        sLog.setEndcode(ENDCODE);
        sLog.setTotalEvents(totalEvents);
        sLog.setShortestTrace(shortestTrace);
        sLog.setLongestTrace(longestTrace);

        return sLog;
    }

    public SimpleLog getSimpleLog(XLog log, XEventClassifier xEventClassifier, double percentage) {
        SimpleLog sLog = getSimpleLog(log, xEventClassifier);
        Map<String, Integer> traces = sLog.getTraces();

        TracesComparator tracesComparator = new TracesComparator(traces);
        TreeMap<String, Integer> sortedTraces = new TreeMap(tracesComparator);
        sortedTraces.putAll(traces);

        int maxTraces = (int) (sLog.size() * percentage);
        int parsed = 0;
        int leastFrequent = 0;

        for( String trace : sortedTraces.keySet() ) {
            if( parsed < maxTraces ) {
//                System.out.println("DEBUG - trace, frequency: " + trace + "," + traces.get(trace) );
                parsed += traces.get(trace);
                leastFrequent = traces.get(trace);
            } else sLog.getTraces().remove(trace);
        }

//        System.out.println("DEBUG - log size: " + sLog.size());
        System.out.println("INFO - log parsed at " + percentage*100 + "%");
//        System.out.println("DEBUG - to parse: " + maxTraces);
//        System.out.println("DEBUG - parsed: " + parsed);
//        System.out.println("DEBUG - min frequency: " + leastFrequent);

        sLog.setSize(parsed);
        return sLog;
    }

    public static SimpleLog getComplexLog(XLog log, XEventClassifier xEventClassifier) {
//        System.out.println("LOGP - starting ... ");
//        System.out.println("LOGP - input log size: " + log.size());

        SimpleLog sLog;

        HashSet<String> labels = new HashSet<>();
        ArrayList<String> orderedLabels;
        HashMap<String, Integer> labelsToIDs = new HashMap<>();  //this maps the original name of an event to its code
        HashMap<Integer, String> events = new HashMap<>();  //this maps the code of the event to its original name
        HashMap<String, Integer> reverseMap = new HashMap<>();  //this maps the event name to its code
        HashMap<String, Integer> traces = new HashMap<>();  //this is the simple log, each trace is a string associated to its frequency

//------------------------------- SPLIT MINER 2.0 -----------------------------
        int totalActivities;

// parallelism keep tracks of the real concurrencies
        int[] parallelism;
        int[] potentialORs;

// when real concurrencies are available, the directly-follow relations slightly differ from the simple case
// requiring to capture them already at this stage
// reminder: matrix[i][j] = array[i*size + j];
        int[] dfg;
        int[] exclusiveness;
        Set<Integer> executed;

// we need to keep track of all the activities that are still executing
// as well as the last activity that was completed
        Set<Integer> executing;
        int lastComplete;
        int endEvent;

        int[] activityObserved;
//-----------------------------------------------------------------------------

        int tIndex; //index to iterate on the log traces
        int eIndex; //index to iterate on the events of the trace

        XTrace trace;
        String sTrace;

        XEvent event;
        String label;

        int LID;
        long totalEvents;
        long oldTotalEvents;

        long startEvents;
        long completeEvents;
        long totalConcurrencies;

        long traceLength;
        long longestTrace = Integer.MIN_VALUE;
        long shortestTrace = Integer.MAX_VALUE;

        int totalTraces = log.size();
        long traceSize;

        events.put(STARTCODE, "autogen-start");
//        NOTE: for complex logs (with activities life-cycle), we overwrite the standard ENDCODE later.
//        events.put(ENDCODE, "autogen-end");

        int count = 0;
        for( tIndex = 0; tIndex < totalTraces; tIndex++ ) {
            /*  we firstly get all the concept names
             *   and we map them into numbers for fast processing
             */

            trace = log.get(tIndex);
            traceSize = trace.size();

            for( eIndex = 0; eIndex < traceSize; eIndex++ ) {
                event = trace.get(eIndex);
//                System.out.println("DEBUG " + count++ + "- lifecycle: " + event.getAttributes().get("lifecycle:transition"));
                label = xEventClassifier.getClassIdentity(event);
                labels.add(label);
            }
        }

        orderedLabels = new ArrayList<>(labels);
        Collections.sort(orderedLabels);

        LID = 1;
        for( String l : orderedLabels ) {
            labelsToIDs.put(l, LID);
            events.put(LID, l);
            reverseMap.put(l, LID);
//            System.out.println("DEBUG - ID:label - " + LID + ":" + l);
            LID++;
        }

//        this plus one accounts for the artificial end event
        totalActivities = events.size()+1;

        potentialORs = new int[totalActivities*totalActivities];
        parallelism = new int[totalActivities*totalActivities];
        dfg = new int[totalActivities*totalActivities];
        activityObserved = new int[totalActivities];
        exclusiveness = new int[totalActivities*totalActivities];
//        this minus one is to ensure we do not go out bound on the array
        endEvent = totalActivities-1;
        events.put(endEvent, "autogen-end");

// reminder: matrix[i][j] = array[i*size + j];
        for(int i = 0; i < totalActivities; i++) {
            activityObserved[i] = 0;
            for (int j = 0; j < totalActivities; j++) {
                dfg[i * totalActivities + j] = 0;
                potentialORs[i * totalActivities + j] = 0;
                parallelism[i * totalActivities + j] = 0;
                exclusiveness[i * totalActivities + j] = 0;
            }
        }

        totalEvents = 0;
        startEvents = 0;
        completeEvents = 0;
        totalConcurrencies = 0;
        for( tIndex = 0; tIndex < totalTraces; tIndex++ ) {
            /* we convert each trace in the log into a string
             *  each string will be a sequence of "::x" terminated with "::", where:
             *  '::' is a separator
             *  'x' is an integer encoding the name of the original event
             */
            trace = log.get(tIndex);
            traceSize = trace.size();

            oldTotalEvents = totalEvents;

            sTrace = "::" + Integer.toString(STARTCODE) + ":";
            lastComplete = STARTCODE;
            executing = new HashSet<>();
            executed = new HashSet<>();
            executed.add(STARTCODE);
            for( eIndex = 0; eIndex < traceSize; eIndex++ ) {
                totalEvents++;
                event = trace.get(eIndex);
                label = xEventClassifier.getClassIdentity(event);
                LID = labelsToIDs.get(label);

//                System.out.println("DEBUG " + count++ + "- lifecycle: " + event.getAttributes().get("lifecycle:transition"));

                if(event.getAttributes().get("lifecycle:transition").toString().equalsIgnoreCase("START")) {
                    startEvents++;
                    for(int e : executing) {
                        if( parallelism[e*totalActivities + LID] == 0 ) totalConcurrencies+=2;
                        parallelism[e*totalActivities + LID]++;
                        parallelism[LID*totalActivities + e]++;
                    }
                    executing.add(LID);
//                    dfg[lastComplete*totalActivities + LID]++;
                    executed.add(LID);
                }

                if(event.getAttributes().get("lifecycle:transition").toString().equalsIgnoreCase("COMPLETE")) {
                    completeEvents++;
                    if( executing.contains(LID) ) executing.remove(LID);
//                    else dfg[lastComplete*totalActivities + LID]++;
                    dfg[lastComplete*totalActivities + LID]++;
                    lastComplete = LID;
                    activityObserved[LID]++;
                    sTrace += ":" + labelsToIDs.get(label).toString() + ":";
                    executed.add(LID);
                }
            }
            dfg[lastComplete*totalActivities + endEvent]++;
            sTrace += ":" + endEvent + "::";
            executed.add(endEvent);

            for(int a=0; a < totalActivities; a++) {
                if(!executed.contains(a)) {
                    for(int x : executed) {
                        exclusiveness[x*totalActivities + a]++;
                        exclusiveness[a*totalActivities + x]++;
                    }
                }
            }

            traceLength = totalEvents - oldTotalEvents;
            if( longestTrace < traceLength ) longestTrace = traceLength;
            if( shortestTrace > traceLength ) shortestTrace = traceLength;

            if( !traces.containsKey(sTrace) ) traces.put(sTrace, 0);
            traces.put(sTrace, traces.get(sTrace)+1);
        }

        System.out.println("LOGP - total events parsed: " + totalEvents);
        System.out.println("LOGP - start events parsed: " + startEvents);
        System.out.println("LOGP - complete events parsed: " + completeEvents);
//        System.out.println("LOGP - total concurrencies identified: " + totalConcurrencies);

        System.out.println("LOGP - total distinct events: " + (events.size() - 2) );
        System.out.println("LOGP - total distinct traces: " + traces.size() );

//        for( String t : traces.keySet() ) System.out.println("DEBUG - ["+ traces.get(t) +"] trace: " + t);

//        System.out.println("DEBUG - final mapping:");
//        for( int code : events.keySet() ) System.out.println("DEBUG - " + code + " = " + events.get(code));

        if( Math.abs(startEvents - completeEvents) < ((double)totalEvents*0.50) ) {
            System.out.println("DEBUG - generating complex log");
            sLog = new ComplexLog(traces, events, log);
            ((ComplexLog)sLog).setDFG(dfg);
            ((ComplexLog)sLog).setConcurrencyMatrix(parallelism);
            sLog.setExclusiveness(exclusiveness);
            ((ComplexLog)sLog).setActivityObserved(activityObserved);
            ((ComplexLog)sLog).computePercentages();

            for(int i=0; i<totalActivities; i++)
                for(int j=0; j<i; j++) {
                    if(exclusiveness[i*totalActivities + j] != 0 && parallelism[i*totalActivities + j] != 0) {
                        potentialORs[i*totalActivities + j]++;
                        potentialORs[j*totalActivities + i]++;
//                        System.out.println("DEBUG - potential OR (" + exclusiveness[i*totalActivities + j] + "," + parallelism[i*totalActivities + j] +") " + "relation: " + events.get(i) + " - "+ i + " and " + events.get(j) + " - "+ j);
                    }
              }

            ((ComplexLog)sLog).setPotentialORs(potentialORs);
        } else {
            sLog = new SimpleLog(traces, events, log);
            sLog.setExclusiveness(exclusiveness);
        }

        sLog.setReverseMap(reverseMap);
        sLog.setStartcode(STARTCODE);
        sLog.setEndcode(endEvent);
        sLog.setTotalEvents(totalEvents);
        sLog.setShortestTrace(shortestTrace);
        sLog.setLongestTrace(longestTrace);
        return sLog;
    }

    private class TracesComparator implements Comparator<String> {
        Map<String, Integer> base;

        public TracesComparator(Map<String, Integer> base) {
            this.base = base;
        }

        @Override
        public int compare(String a, String b) {
            if (base.get(a) >= base.get(b)) return -1;
            else return 1;
            // returning 0 would merge keys
        }
    }

}