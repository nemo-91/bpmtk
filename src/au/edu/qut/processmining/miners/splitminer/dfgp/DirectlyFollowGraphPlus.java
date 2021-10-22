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

package au.edu.qut.processmining.miners.splitminer.dfgp;

import au.edu.qut.processmining.log.ComplexLog;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramImpl;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;

import java.util.*;

/**
 * Created by Adriano on 24/10/2016.
 */
public class DirectlyFollowGraphPlus {

    public enum Gate {SAND, AND, OR, XOR};

    private static boolean completeCloning = false;

    private SimpleLog log;
    private int startcode;
    private int endcode;

    private Set<DFGEdge> edges;
    private Map<Integer, DFGNode> nodes;
    private Map<Integer, HashSet<DFGEdge>> outgoings;
    private Map<Integer, HashSet<DFGEdge>> incomings;
    private Map<Integer, HashMap<Integer, DFGEdge>> dfgp;

    private Set<Integer> loopsL1;
    private Map<Integer, Integer> loopsL1Freq;
    private Set<DFGEdge> loopsL2;
    private Map<Integer, HashSet<Integer>> parallelisms;
    private double[] concurrencyMatrix;
    private Set<DFGEdge> bestEdges;
    private Set<DFGEdge> untouchableEdges;
    private Set<DFGEdge> potentialConcurrency;
    private HashMap<Pair<Integer,Integer>, Gate> relations = null;

    private double percentileFrequencyThreshold;
    private double parallelismsThreshold;
//    this threshold is used only for ComplexLogs in input
    private DFGPUIResult.FilterType filterType;
    private int filterThreshold;
//    private boolean percentileOnBest;
    private boolean parallelismsFirst;

    private boolean oracle = false;


    protected DirectlyFollowGraphPlus() {}

    public DirectlyFollowGraphPlus(SimpleLog log) {
        this(log, DFGPUIResult.FREQUENCY_THRESHOLD, DFGPUIResult.PARALLELISMS_THRESHOLD, DFGPUIResult.STD_FILTER, DFGPUIResult.PARALLELISMS_FIRST);
    }

    public DirectlyFollowGraphPlus(SimpleLog log, double percentileFrequencyThreshold, double parallelismsThreshold, DFGPUIResult.FilterType filterType, boolean parallelismsFirst, double auxiliary) {
        this(log, percentileFrequencyThreshold, parallelismsThreshold, filterType, parallelismsFirst);
    }

    public DirectlyFollowGraphPlus(SimpleLog log, double percentileFrequencyThreshold, double parallelismsThreshold, DFGPUIResult.FilterType filterType, boolean parallelismsFirst) {
        this.log = log;
        this.startcode = log.getStartcode();
        this.endcode = log.getEndcode();
        this.percentileFrequencyThreshold = percentileFrequencyThreshold;
        this.parallelismsThreshold = parallelismsThreshold;
        this.filterType = percentileFrequencyThreshold == 0 ? DFGPUIResult.FilterType.NOF : filterType;
//        this.percentileOnBest = percentileOnBest;
        this.parallelismsFirst = parallelismsFirst;
    }

    public DirectlyFollowGraphPlus(DirectlyFollowGraphPlus directlyFollowGraphPlus) {
        this.startcode = directlyFollowGraphPlus.startcode;
        this.endcode = directlyFollowGraphPlus.endcode;
        this.edges = new HashSet<>(directlyFollowGraphPlus.edges);
        this.nodes = directlyFollowGraphPlus.nodes;
        this.loopsL1 = directlyFollowGraphPlus.loopsL1;

        if(completeCloning) {
            this.log = directlyFollowGraphPlus.log;
            this.loopsL2 = directlyFollowGraphPlus.loopsL2;
            this.parallelisms = directlyFollowGraphPlus.parallelisms;
            this.bestEdges = directlyFollowGraphPlus.bestEdges;
            this.percentileFrequencyThreshold = directlyFollowGraphPlus.percentileFrequencyThreshold;
            this.parallelismsThreshold = directlyFollowGraphPlus.parallelismsThreshold;
            this.filterType = directlyFollowGraphPlus.filterType;
            this.filterThreshold = directlyFollowGraphPlus.filterThreshold;
            this.parallelismsFirst = directlyFollowGraphPlus.parallelismsFirst;
        }
    }


    public void setOracle(boolean oracle) { this.oracle = oracle; }
    public int size() { return nodes.size(); }
    public Set<DFGEdge> getEdges() { return edges; }
    public SimpleLog getSimpleLog() { return log; }
    public int getStartcode() { return startcode; }
    public int getEndcode() { return endcode; }
    public Set<Integer> getLoopsL1() { return loopsL1; }
    public Map<Integer, HashSet<Integer>> getParallelisms() { return parallelisms; }
    public int[] getConcurrencyMatrix() {
        if( log instanceof ComplexLog ) return ((ComplexLog) log).getConcurrencyMatrix();
        else return new int[log.getEvents().size()*log.getEvents().size()];
    }

    public BPMNDiagram getDFG() {
        buildDirectlyFollowsGraph();
        return getDFGP(true);
    }

    public BPMNDiagram getDFGP(boolean labels) {
        Map<Integer, String> events = log.getEvents();
        BPMNDiagram diagram = new BPMNDiagramImpl("DFGP-diagram");
        HashMap<Integer, BPMNNode> mapping = new HashMap<>();
        String label;
        Activity task;
        BPMNNode src, tgt;

        for( int event : nodes.keySet() ) {
            label = events.get(event) + "\n(" + nodes.get(event).getFrequency() + ")";
            task = diagram.addActivity( (labels ? label : Integer.toString(event)), false, false, false, false, false);
            mapping.put(event, task);
        }

        for( DFGEdge edge : edges ) {
            src = mapping.get(edge.getSourceCode());
            tgt = mapping.get(edge.getTargetCode());
            diagram.addFlow(src, tgt, edge.toString());
        }

        return diagram;
    }

    public BPMNDiagram convertIntoBPMNDiagram() {
        BPMNDiagram diagram = new BPMNDiagramImpl("eDFGP-diagram");
        HashMap<Integer, BPMNNode> mapping = new HashMap<>();
        String label;
        BPMNNode node;
        BPMNNode src, tgt;

        for( int event : nodes.keySet() ) {
            label = Integer.toString(event);

            if( event == startcode || event == endcode )
                node = diagram.addEvent(label, (event == startcode ? Event.EventType.START : Event.EventType.END), Event.EventTrigger.NONE, (event == startcode ? Event.EventUse.CATCH : Event.EventUse.THROW), true, null);
            else
                node = diagram.addActivity(label, loopsL1.contains(event), false, false, false, false);

            mapping.put(event, node);
        }

        for( DFGEdge edge : edges ) {
            src = mapping.get(edge.getSourceCode());
            tgt = mapping.get(edge.getTargetCode());
            diagram.addFlow(src, tgt, edge.toString());
        }

        return diagram;
    }

    public BPMNDiagram convertIntoBPMNDiagramWithOriginalLabels() {
        BPMNDiagram diagram = new BPMNDiagramImpl("eDFGP-diagram");
        HashMap<Integer, BPMNNode> mapping = new HashMap<>();
        String label;
        BPMNNode node;
        BPMNNode src, tgt;

        for( int event : nodes.keySet() ) {
            label = nodes.get(event).getLabel();

            if( event == startcode || event == endcode )
                node = diagram.addEvent(label, (event == startcode ? Event.EventType.START : Event.EventType.END), Event.EventTrigger.NONE, (event == startcode ? Event.EventUse.CATCH : Event.EventUse.THROW), true, null);
            else
                node = diagram.addActivity(label, loopsL1.contains(event), false, false, false, false);

            mapping.put(event, node);
        }

        for( DFGEdge edge : edges ) {
            src = mapping.get(edge.getSourceCode());
            tgt = mapping.get(edge.getTargetCode());
            diagram.addFlow(src, tgt, edge.toString());
        }

        return diagram;
    }

    public boolean areConcurrent(int A, int B) {
//        if( log instanceof ComplexLog ) return concurrencyMatrix[A*log.getEvents().size() +B] > parallelismsThreshold;
        return (parallelisms.containsKey(A) && parallelisms.get(A).contains(B));
    }

    public int[] getPotentialORs() {
        if(log instanceof ComplexLog) return ((ComplexLog) log).getPotentialORs();

        int[] exclusiveness = log.getExclusiveness();
        int length = exclusiveness.length;
        int[] potentialORs = new int[length];
        int size = (int) Math.sqrt(length);

        for(int i=0; i<length; i++)
            if(areConcurrent(i/size,i%size) && exclusiveness[i] > 0) potentialORs[i] = 1;

        return potentialORs;
    }

    public boolean areInclusive(int A, int B) {
        if( relations != null  && relations.get(new ImmutablePair<>(A,B)) == Gate.OR ) return true;
        else return false;
    }

    public void buildDFGP() {
        System.out.println("DFGP - settings (eta, epsilon, filter-type) > " + percentileFrequencyThreshold + " : " + parallelismsThreshold + " : " + filterType.toString());
        untouchableEdges = null;

        if(log instanceof ComplexLog) {
            buildDFGfromComplexLog();
            detectLoops();          //depends on buildDirectlyFollowsGraph()
            detectParallelismsFromComplexLog();
        } else {
            buildDirectlyFollowsGraph();
            detectLoops();          //depends on buildDirectlyFollowsGraph()
            if(oracle) detectRelationsOnLog();
            else detectParallelismsOnDFG();   //depends on detectLoops()
        }

//        removeEventSubprocesses();
        switch(filterType) {                        //depends on detectParallelisms()
            case FWG:
                filterWithGuarantees();
                break;
            case WTH:
                filterWithThreshold();
                exploreAndRemove();
                break;
            case STD:
                standardFilter();
                exploreAndRemove();
                break;
            case NOF:
//                filterWithGuarantees();
//                exploreAndRemove();
                break;
        }
    }

    public void buildSafeDFGP() {
        System.out.println("DFGP - settings (eta, epsilon, filter-type) > " + percentileFrequencyThreshold + " : " + parallelismsThreshold + " : " + filterType.toString());

        buildDirectlyFollowsGraph();                //first method to execute
        bestEdgesOnMaxCapacitiesForConnectedness(); //this ensure a strongly connected graph (density may be impaired)
        detectLoops();                              //depends on buildDirectlyFollowsGraph()
        detectParallelismsOnDFG();                       //depends on detectLoops()

        switch(filterType) {                        //depends on detectParallelisms()
            case FWG:
                filterWithGuarantees();
                break;
            case WTH:
                filterWithThreshold();
                exploreAndRemove();
                break;
            case STD:
                standardFilter();
                exploreAndRemove();
                break;
            case NOF:
//                filterWithGuarantees();
//                exploreAndRemove();
                break;
        }

    }

    public void buildDFGfromComplexLog() {
        ComplexLog cLog = (ComplexLog) log;
        long removedEdges = 0;

        nodes = new HashMap<>();
        edges = new HashSet<>();
        outgoings = new HashMap<>();
        incomings = new HashMap<>();
        dfgp = new HashMap<>();
        potentialConcurrency = new HashSet<>();
        loopsL1 = new HashSet<>();

//        cLog.printConcurrencyMatrix();
//        cLog.printRelativeConcurrencyMatrix();
        this.concurrencyMatrix = cLog.getRelativeConcurrencyMatrix();

        double[] relativeDFG = cLog.getRelativeDFG(); // not recommended its usage (21 Apr 2020)
        int[] dfg = cLog.getDFG();
        Map<Integer, String> events = cLog.getEvents();
        int totalActivities = events.size();

        DFGNode node;
        DFGEdge edge;

//        cLog.printDFG();
//        cLog.printRelativeDFG();
        for(int k=0; k < totalActivities; k++) {
            node =  new DFGNode(events.get(k), k);
            this.addNode(node);
        }

        for(int i=0; i < totalActivities; i++)
            for(int j=0; j < totalActivities; j++)
                if( dfg[i*totalActivities + j] > 0 ) {
//                if( relativeDFG[i*totalActivities + j] > 0.1 ) {
                    if(i==j) loopsL1.add(i);
                    else {
                        edge = new DFGEdge(nodes.get(i), nodes.get(j), dfg[i * totalActivities + j]);
                        this.addEdge(edge);
                        if (concurrencyMatrix[i * totalActivities + j] > parallelismsThreshold) {
                            potentialConcurrency.add(edge);
                            removedEdges++;
                        }
                    }
                }

        for(int k=0; k < totalActivities; k++)
            if(incomings.get(k).size() == outgoings.get(k).size() && outgoings.get(k).size() == 0) {
                System.out.println("DEBUG - node removed: "+ events.get(k) +" (" + k + ")");
                this.removeNode(k);
            }

        System.out.println("DEBUG - potential parallelisms: " + removedEdges);
    }

    public void buildDirectlyFollowsGraph() {
        Map<String, Integer> traces = log.getTraces();
        Map<Integer, String> events = log.getEvents();

        StringTokenizer trace;
        int traceFrequency;

        int event;
        int prevEvent;

        DFGNode node;
        DFGNode prevNode;
        DFGEdge edge;

        DFGNode autogenStart;
        DFGNode autogenEnd;

        nodes = new HashMap<>();
        edges = new HashSet<>();
        outgoings = new HashMap<>();
        incomings = new HashMap<>();
        dfgp = new HashMap<>();
        loopsL1 = new HashSet<>();
        loopsL1Freq = new HashMap<>();

        autogenStart = new DFGNode(events.get(startcode), startcode);
        this.addNode(autogenStart);
//        while parsing the simple log we will always skip the start event,
//        so we set now the maximum frequency because it is an artificial start event
        autogenStart.increaseFrequency(log.size());

        autogenEnd = new DFGNode(events.get(endcode), endcode);
        this.addNode(autogenEnd);

        for( String t : traces.keySet() ) {
            trace = new StringTokenizer(t, "::");
            traceFrequency = traces.get(t);

//            consuming the start event that is always 0
            trace.nextToken();
            prevEvent = startcode;
            prevNode = autogenStart;

            while( trace.hasMoreTokens() ) {
//                we read the next event of the trace until it is finished
                event = Integer.valueOf(trace.nextToken());

                if(prevEvent == event) {
                    if(loopsL1.contains(event)) loopsL1Freq.put(event, (loopsL1Freq.get(event)+1));
                    else {
                        loopsL1.add(event);
                        loopsL1Freq.put(event, 1);
                    }
                    continue;
                }

                if( !nodes.containsKey(event) ) {
                    node =  new DFGNode(events.get(event), event);
                    this.addNode(node);
                } else node = nodes.get(event);

//                  increasing frequency of this event occurrence
                node.increaseFrequency(traceFrequency);

                if( !dfgp.containsKey(prevEvent) || !dfgp.get(prevEvent).containsKey(event) ) {
                    edge = new DFGEdge(prevNode, node);
                    this.addEdge(edge);
                }

//                  increasing frequency of this directly following relationship
                dfgp.get(prevEvent).get(event).increaseFrequency(traceFrequency);

                prevEvent = event;
                prevNode = node;
            }
        }
    }

    public void detectLoops() {
        Map<String, Integer> traces = log.getTraces();
        HashSet<DFGEdge> removableLoopEdges = new HashSet();

        DFGEdge e2;
        int src;
        int tgt;

        String src2tgt_loop2Pattern;
        String tgt2src_loop2Pattern;

        int src2tgt_loop2Frequency;
        int tgt2src_loop2Frequency;

        int loop2score;

        loopsL2 = new HashSet<>();
        for( DFGEdge e1 : edges )  {
            src = e1.getSourceCode();
            tgt = e1.getTargetCode();

//            if src OR tgt are length 1 loops, we do not evaluate length 2 loops for this edge,
//            because a length 1 loop in parallel with something else
//            can generate pattern of the type [src :: tgt :: src] OR [tgt :: src :: tgt]
            if( !loopsL2.contains(e1) && dfgp.get(tgt).containsKey(src) && !loopsL1.contains(src) && !loopsL1.contains(tgt) ) {
                e2 = dfgp.get(tgt).get(src);

                src2tgt_loop2Pattern = "::" + src + "::" + tgt + "::" + src + "::";
                tgt2src_loop2Pattern = "::" + tgt + "::" + src + "::" + tgt + "::";
                src2tgt_loop2Frequency = 0;
                tgt2src_loop2Frequency = 0;

                for( String trace : traces.keySet() ) {
                    src2tgt_loop2Frequency += (StringUtils.countMatches(trace, src2tgt_loop2Pattern)*traces.get(trace));
                    tgt2src_loop2Frequency += (StringUtils.countMatches(trace, tgt2src_loop2Pattern)*traces.get(trace));
                }

                loop2score = src2tgt_loop2Frequency + tgt2src_loop2Frequency;

//                if the loop2score is not zero, it means we found patterns of the type:
//                [src :: tgt :: src] OR [tgt :: src :: tgt], so we set both edges as short-loops
                if( loop2score != 0 ) {
                    loopsL2.add(e1);
                    loopsL2.add(e2);
                }
            }
        }

        System.out.println("DFGP - loops length TWO found: " + loopsL2.size()/2);
    }

    public void detectParallelismsFromComplexLog() {
        int confirmedParallelisms = 0;

        int src;
        int tgt;

        parallelisms = new HashMap<>();

        ArrayList<DFGEdge> orderedRemovableEdges = new ArrayList<>(potentialConcurrency);
        Collections.sort(orderedRemovableEdges);
        while( !orderedRemovableEdges.isEmpty() ) {
            DFGEdge re = orderedRemovableEdges.remove(0);
            src = re.getSourceCode();
            tgt = re.getTargetCode();
            if (!this.removeEdge(re, true)) {
//                System.out.println("DEBUG - impossible remove: " + re.print());
                if (parallelisms.containsKey(src)) parallelisms.get(src).remove(tgt);
                if (parallelisms.containsKey(tgt)) parallelisms.get(tgt).remove(src);
                if ((re = dfgp.get(tgt).get(src)) != null) this.removeEdge(re, true);
            } else {
                confirmedParallelisms++;
                if (!parallelisms.containsKey(src)) parallelisms.put(src, new HashSet<>());
                parallelisms.get(src).add(tgt);
                if (!parallelisms.containsKey(tgt)) parallelisms.put(tgt, new HashSet<>());
                parallelisms.get(tgt).add(src);
            }
        }

        System.out.println("DEBUG - removed parallelism edges: " + confirmedParallelisms);
    }

    public void detectRelationsOnLog() {
        int confirmedParallelisms = 0;

        int src;
        int tgt;

        int s1, s2;
        DFGEdge e;

        relations = new HashMap<>();
        potentialConcurrency = new HashSet<>();
        parallelisms = new HashMap<>();
        generateBitmatrixSplits();

        for( Pair<Integer,Integer> p : relations.keySet() )
            if(relations.get(p) == Gate.AND || relations.get(p) == Gate.OR) {
                s1 = p.getLeft();
                s2 = p.getRight();

                e = dfgp.get(s1).get(s2);
                if(e != null) potentialConcurrency.add(e);

                e = dfgp.get(s2).get(s1);
                if(e != null) potentialConcurrency.add(e);
            }

        System.out.println("DEBUG - total potential concurrencies: " + potentialConcurrency.size());

        ArrayList<DFGEdge> orderedRemovableEdges = new ArrayList<>(potentialConcurrency);
        Collections.sort(orderedRemovableEdges);
        while( !orderedRemovableEdges.isEmpty() ) {
            DFGEdge re = orderedRemovableEdges.remove(0);
            src = re.getSourceCode();
            tgt = re.getTargetCode();
            if (!this.removeEdge(re, true)) {
//                System.out.println("DEBUG - impossible remove: " + re.print());
                if (parallelisms.containsKey(src)) parallelisms.get(src).remove(tgt);
                if (parallelisms.containsKey(tgt)) parallelisms.get(tgt).remove(src);
                if ((re = dfgp.get(tgt).get(src)) != null) this.removeEdge(re, true);
            } else {
                confirmedParallelisms++;
                if (!parallelisms.containsKey(src)) parallelisms.put(src, new HashSet<>());
                parallelisms.get(src).add(tgt);
                if (!parallelisms.containsKey(tgt)) parallelisms.put(tgt, new HashSet<>());
                parallelisms.get(tgt).add(src);
            }
        }

        System.out.println("DEBUG - removed parallelism edges: " + confirmedParallelisms);
    }

    private void generateBitmatrixSplits() {
//      METHOD-DEPENDENT DATA STRUCTURES

//        for each split task, we have a matrix of bits, each column being a combination of successors tasks that are executed
        HashMap<Integer, Matrix> splitMaps = new HashMap<>();
//        for each split task, we have an array storing the successors ids
//        we query the array to know the index of the successor to update accordingly the row of a matrix
        HashMap<Integer, ArrayList<Integer>> successors = new HashMap<>();

//        these two structures keep track of the mapping between integer successors codes and the BPMN nodes
//        as well as the incoming edge of each successor, this is fundamental to edit the BPMN diagram later
        HashMap<Integer, Integer> successorsToNodes = new HashMap<>();
        HashMap<Integer, DFGEdge> successorsToEdges = new HashMap<>();

//      TRACE-DEPENDENT DATA STRUCTURES

//        while parsing each trace, we have to keep track of all the split tasks that we encountered
//        then for each of them, if we see one of their successors, we update their bit set
        HashMap<Integer, BitSet> splitTasksInTrace = new HashMap<>();
//        for each encountered split task, we remember how far in time we encountered it
//        we can set a max distance after which we do not update anymore the bitarray for that split task
        HashMap<Integer, Integer> distances = new HashMap<>();


        StringTokenizer trace;
        int traceFrequency;
        int event;
//        int MAXD = 5;
        int MAXD = Integer.MAX_VALUE;
        int skipcounter = 0;
        int i;

        Map<String, Integer> traces = log.getTraces();

        int size;
        int SID; // tmp successors ID
        ArrayList<Integer> tmpSuccessors;
        for(int TID : nodes.keySet())
            if((size = outgoings.get(TID).size()) > 1) {
                splitMaps.put(TID, new Matrix(size));

                tmpSuccessors = new ArrayList<>(size);
                for(DFGEdge e : outgoings.get(TID)) {
                    SID = e.getTargetCode();
                    tmpSuccessors.add(SID);
                    splitMaps.get(TID).addSuccessor(SID);
//                    successorsToNodes.put(SID, e.getTargetCode());
//                    successorsToEdges.put(SID, e);
                }
                successors.put(TID, tmpSuccessors);
            }


        for( String t : traces.keySet() ) {
            trace = new StringTokenizer(t, "::");
            traceFrequency = traces.get(t);
            splitTasksInTrace.clear();

//            consuming the start event that is always 0
//            we assume that the start event is not a successor of any split or a split itself
            trace.nextToken();
            while( trace.hasMoreTokens() ) {
                event = Integer.valueOf(trace.nextToken());
                if (splitMaps.containsKey(event)) {
                    distances.put(event, 0); // not sure we need this, for the moment we keep it
                    if (!splitTasksInTrace.containsKey(event)) splitTasksInTrace.put(event, new BitSet());
                }

                for( int sti : splitTasksInTrace.keySet() ) {
//                    we now scan all the split tasks that we encountered so far
//                    however, split task executed too long ago or same of the current event are not taken into account
//                    distances.put(sti, distances.get(sti)+1);
                    if( distances.get(sti) > MAXD ||  event == sti ) {
                        skipcounter++;
                        continue;
                    }
//                    if the current event is a successor for one or more of them (in which case the event is also a join)
//                    we update the observation bitset of the split task for this trace
                    if( (i = successors.get(sti).indexOf(event)) != -1 ) splitTasksInTrace.get(sti).set(i);
                }
            }

//            once the whole trace has been parsed, we add the bitset of each split task to its matrix of bitsets
            for( int sti : splitTasksInTrace.keySet() ) splitMaps.get(sti).addBitset(splitTasksInTrace.get(sti), traceFrequency);
        }

        for( int sti : splitMaps.keySet() )
            generateSplitsHierarchyFromObservationMatrix(sti, splitMaps.get(sti));

        System.out.println("DEBUG - skipcounter = " + skipcounter);
    }

    private void generateSplitsHierarchyFromObservationMatrix(int split, Matrix matrix) {
        boolean print = false;

        System.out.println("DEBUG - split task: " + log.getEvents().get(split) + " (" + split + ")");
//        matrix.print();
        matrix.prune(parallelismsThreshold);

        HashMap<Integer, BitSet> transposedMatrix = new HashMap<>();
        int ROWS = matrix.rows();

//      first we transpose the matrix
        for(int i = 0; i< matrix.totalSuccessors(); i++)
            transposedMatrix.put(matrix.successors[i], new BitSet(ROWS));

        int r = 0; // row = bitset
        for(BitSet bs : matrix.matrix.keySet()) {
            int sl = 0; // location of the successor in the current biset
            for( ; sl<matrix.totalSuccessors(); sl++)
                transposedMatrix.get(matrix.successors[sl]).set(r, bs.get(sl));
            r++;
        }
//      transposition is over

        if(print) {
            System.out.println("DEBUG - printing transposed matrix:");
            for (int s : transposedMatrix.keySet()) {
                System.out.print("S: " + s + " :");
                for (int si = 0; si < ROWS; si++) {
                    if (transposedMatrix.get(s).get(si)) System.out.print("1");
                    else System.out.print("0");
                }
                System.out.println();
            }
        }

//        we compare the successors observations in the same trace
        Set<Pair<Integer,Integer>> ANDs = new HashSet<>();
        Set<Pair<Integer,Integer>> SANDs = new HashSet<>();
        Set<Pair<Integer,Integer>> XORs = new HashSet<>();
        Set<Pair<Integer,Integer>> ORs = new HashSet<>();
        Set<Integer> skips = new HashSet<>();
        BitSet bs0, bs1, bs2;
        Set<Integer> removableSuccessors = new HashSet<>();
        Set<Integer> analysed = new HashSet<>();
        Gate type;

        System.out.println("DEBUG - discovering relations");
        for (int s1 : transposedMatrix.keySet()) {
            bs1 = transposedMatrix.get(s1);
            analysed.add(s1);
            for (int s2 : transposedMatrix.keySet()) {
                if( analysed.contains(s2) ) continue;
                bs2 = transposedMatrix.get(s2);
                type = determineRelation(s1, s2, bs1, bs2, ROWS, skips);
                switch (type) {
                    case SAND:
                        SANDs.add(new ImmutablePair<>(s1,s2));
                        break;
                    case AND:
                        ANDs.add(new ImmutablePair<>(s1,s2));
                        break;
                    case XOR:
                        XORs.add(new ImmutablePair<>(s1,s2));
                        break;
                    case OR:
                        ORs.add(new ImmutablePair<>(s1,s2));
                        break;
                }
            }
        }

//            first we check if we found ANDs
        for(Pair<Integer,Integer> p : ANDs) {
//                if(removableSuccessors.contains(p.getLeft()) || removableSuccessors.contains(p.getRight())) continue;
//                when we find an AND we can remove one of the two without any issues
            bs1 = transposedMatrix.get(p.getLeft());
            removableSuccessors.add(p.getLeft());
            removableSuccessors.add(p.getRight());
            bs0 = new BitSet();
            for(int i = 0; i<ROWS; i++) bs0.set(i, bs1.get(i));
//                transposedMatrix.put(p.getLeft()*100, bs0);
//            System.out.println("DEBUG - AND("+ p.getLeft() + "," + p.getRight() + ")");
            if(relations.containsKey(p) && relations.get(p) != Gate.AND) System.out.println("WARNING - double relation for: ("+ p.getLeft() + "," + p.getRight() + ")");
            relations.put(p, Gate.AND);
        }

//            then we check for XORs
        for (Pair<Integer, Integer> p : XORs) {
//                if(removableSuccessors.contains(p.getLeft()) || removableSuccessors.contains(p.getRight())) continue;
//            System.out.println("DEBUG - XOR("+ p.getLeft() + "," + p.getRight() + ")");
//                when we find an XOR....
            bs1 = transposedMatrix.get(p.getLeft());
            bs2 = transposedMatrix.get(p.getRight());
            removableSuccessors.add(p.getLeft());
            removableSuccessors.add(p.getRight());
            bs0 = new BitSet();
            for(int i = 0; i<ROWS; i++) bs0.set(i, bs1.get(i) || bs2.get(i));
//                transposedMatrix.put(p.getLeft()*100, bs0);
            if(relations.containsKey(p) && relations.get(p) != Gate.XOR) System.out.println("WARNING - double relation for: ("+ p.getLeft() + "," + p.getRight() + ")");
            relations.put(p, Gate.XOR);
        }

//            then we consider ORs
        for (Pair<Integer, Integer> p : ORs) {
//                if(removableSuccessors.contains(p.getLeft()) || removableSuccessors.contains(p.getRight())) continue;
//            System.out.println("DEBUG - OR("+ p.getLeft() + "," + p.getRight() + ")");
//                when we find an OR...
            bs1 = transposedMatrix.get(p.getLeft());
            bs2 = transposedMatrix.get(p.getRight());
            removableSuccessors.add(p.getLeft());
            removableSuccessors.add(p.getRight());
            bs0 = new BitSet();
            for(int i = 0; i<ROWS; i++) bs0.set(i, bs1.get(i) || bs2.get(i));
//                transposedMatrix.put(p.getLeft()*100, bs0);
            if(relations.containsKey(p) && relations.get(p) != Gate.OR) System.out.println("WARNING - double relation for: ("+ p.getLeft() + "," + p.getRight() + ")");
            relations.put(p, Gate.OR);
        }

/*      NOT USED ATM                 finally we consider ANDs with skips
        for (Pair<Integer, Integer> p : SANDs) {
//                if(removableSuccessors.contains(p.getLeft()) || removableSuccessors.contains(p.getRight())) continue;
            bs1 = transposedMatrix.get(p.getLeft());
            bs2 = transposedMatrix.get(p.getRight());
            removableSuccessors.add(p.getLeft());
            removableSuccessors.add(p.getRight());
            bs0 = new BitSet();

            if( skips.contains(p.getLeft()) ) {
                System.out.println("DEBUG - AND(SKIP(" + p.getLeft() + ")," + p.getRight() + ")");
                for (int i = 0; i < ROWS; i++) bs0.set(i, bs2.get(i));
            } else {
                System.out.println("DEBUG - AND("+ p.getLeft() + ",SKIP(" + p.getRight() + "))");
                for(int i = 0; i<ROWS; i++) bs0.set(i, bs1.get(i));
            }
//                transposedMatrix.put(p.getLeft()*100, bs0);
        }
*/

        for( int rs : removableSuccessors ) transposedMatrix.remove(rs);
    }

    private Gate determineRelation(int s1, int s2, BitSet bs1, BitSet bs2, int size, Set<Integer> skips) {
        Gate type = Gate.OR;
        boolean safe = true;
        int mismatch = 0;
        int match = 0;

        if(loopsL2.contains(dfgp.get(s1).get(s2))) {
            System.out.println("DEBUG - pair ("+ s1 + "," + s2 + ") - shortloop");
            return Gate.XOR;
        }

        Set<Pair<Boolean, Boolean>> observations = new HashSet<>();
        for(int i = 0; i<size; i++)
            observations.add(new ImmutablePair<>(bs1.get(i),bs2.get(i)));

//        System.out.println("DEBUG - obs for ("+ s1 + "," + s2 + "): " + observations.size());

        size = observations.size();
        if( observations.remove(new ImmutablePair<>(false, false)) ) size--;
        if(observations.size() >= 3) return Gate.OR;

//        if all the observations match, we have an AND
        for(Pair<Boolean, Boolean> p : observations)
            if(p.getLeft() == p.getRight()) match++;
            else mismatch++;

        if(match == size) return Gate.AND;
        if(safe && (s1 < 0) || (s2 < 0) ) return Gate.XOR; // this is to play safe with successor-joins

//        otherwise we could have a XOR or OR
        Pair<Boolean, Boolean> skipL = new ImmutablePair<>(false, true);
        Pair<Boolean, Boolean> skipR = new ImmutablePair<>(true, false);
        Pair<Boolean, Boolean> skipNone = new ImmutablePair<>(true, true);
//        Pair<Boolean, Boolean> skipBoth = new ImmutablePair<>(false, false);

        if(observations.contains(skipL) && observations.contains(skipNone)) {
            skips.add(s1);
            return Gate.XOR;
        }

        if(observations.contains(skipR) && observations.contains(skipNone)) {
            skips.add(s2);
            return Gate.XOR;
        }

        if(observations.contains(skipL) && observations.contains(skipR)) return Gate.XOR;
        else return Gate.OR;


    }

    public void detectParallelismsOnDFG() {
//        int totalParallelisms = 0;
//        int confirmedParallelisms = 0;
//        int notParallel = 0;
        boolean priorityCheck;

        DFGEdge e2;
        int src;
        int tgt;

        int src2tgt_frequency;
        int tgt2src_frequency;
        double parallelismScore;

        HashSet<DFGEdge> removableEdges = new HashSet<>();

        parallelisms = new HashMap<>();

        if( parallelismsThreshold == 0 ) return;

        for (DFGEdge e1 : edges) {
            src = e1.getSourceCode();
            tgt = e1.getTargetCode();

            if( parallelismsFirst ) priorityCheck = !loopsL2.contains(e1);
            else priorityCheck = !loopsL2.contains(e1) && !loopsL1.contains(src) && !loopsL1.contains(tgt);

            if( dfgp.get(tgt).containsKey(src) && priorityCheck && !removableEdges.contains(e1)) {
//                this means: src || tgt is candidate parallelism
                    e2 = dfgp.get(tgt).get(src);

                    src2tgt_frequency = e1.getFrequency();
                    tgt2src_frequency = e2.getFrequency();
                    parallelismScore = (double) (src2tgt_frequency - tgt2src_frequency) / (src2tgt_frequency + tgt2src_frequency);

                    if (Math.abs(parallelismScore) < parallelismsThreshold) {
//                    if parallelismScore is less than the threshold epslon,
//                    we set src || tgt and vice-versa, and we remove e1 and e2
                        if (!parallelisms.containsKey(src)) parallelisms.put(src, new HashSet<Integer>());
                        parallelisms.get(src).add(tgt);
                        if (!parallelisms.containsKey(tgt)) parallelisms.put(tgt, new HashSet<Integer>());
                        parallelisms.get(tgt).add(src);
                        removableEdges.add(e1);
                        removableEdges.add(e2);
//                        totalParallelisms+=2;
                    } else {
//                    otherwise we remove the least frequent edge, e1 or e2
                        if (parallelismScore > 0) removableEdges.add(e2);
                        else removableEdges.add(e1);
//                        notParallel++;
                    }
            }
        }

        ArrayList<DFGEdge> orderedRemovableEdges = new ArrayList<>(removableEdges);
        Collections.sort(orderedRemovableEdges);
        while( !orderedRemovableEdges.isEmpty() ) {
            DFGEdge re = orderedRemovableEdges.remove(0);
            if( !this.removeEdge(re, true) ) {
//                System.out.println("DEBUG - impossible remove: " + re.print());
                src = re.getSourceCode();
                tgt = re.getTargetCode();
                if( parallelisms.containsKey(src) ) parallelisms.get(src).remove(tgt);
                if( parallelisms.containsKey(tgt) ) parallelisms.get(tgt).remove(src);
                if( (re = dfgp.get(tgt).get(src)) != null ) this.removeEdge(re, true);
            }
//            else { confirmedParallelisms++; }
        }

//        System.out.println("DFGP - parallelisms found (total, confirmed): (" + totalParallelisms + " , " + confirmedParallelisms + ")");
    }

    public void removeEventSubprocesses() {
        int maxP = (int) (nodes.size() * 0.30);
        HashSet<Integer> eventSubprocesses = new HashSet<>();

        System.out.println("DEBUG - max parallelisms allowed: " + maxP );
        for(int n : nodes.keySet())
            if(parallelisms.containsKey(n) && parallelisms.get(n).size() >= maxP) eventSubprocesses.add(n);
        System.out.println("DEBUG - total event subprocesses: " + eventSubprocesses.size());

        for(int es : eventSubprocesses) this.removeNode(es);

        exploreAndRemove();
    }

    private void standardFilter() {
        int src;
        int tgt;
        DFGEdge recoverableEdge;

        bestEdgesOnMaxFrequencies();
        ArrayList<DFGEdge> frequencyOrderedBestEdges = new ArrayList<>(bestEdges);

        for( DFGEdge e : new HashSet<>(edges) ) this.removeEdge(e, false);

        Collections.sort(frequencyOrderedBestEdges);
        for( int i = (frequencyOrderedBestEdges.size()-1); i >= 0; i-- ) {
            recoverableEdge = frequencyOrderedBestEdges.get(i);

            src = recoverableEdge.getSourceCode();
            tgt = recoverableEdge.getTargetCode();
            if( outgoings.get(src).isEmpty() || incomings.get(tgt).isEmpty() ) this.addEdge(recoverableEdge);
        }
    }

    private void bestEdgesOnMaxFrequencies() {
        bestEdges = new HashSet<>();

        for( int node : nodes.keySet() ) {
            if( node != endcode ) {
//                System.out.println("DEBUG - node (max, " + endcode + " ): " + node);
                bestEdges.add(Collections.max(outgoings.get(node)));
            }
            if( node != startcode ) bestEdges.add(Collections.max(incomings.get(node)));
        }
    }

    private void filterWithThreshold() {
        int src;
        int tgt;
        DFGEdge recoverableEdge;

        bestEdgesOnMaxFrequencies();
        computeFilterThreshold();

        ArrayList<DFGEdge> orderedMostFrequentEdges = new ArrayList<>(bestEdges);

        for( DFGEdge e : orderedMostFrequentEdges ) this.removeEdge(e, false);
        for( DFGEdge e : new HashSet<>(edges) ) {
            if( e.getFrequency() > filterThreshold) orderedMostFrequentEdges.add(e);
            this.removeEdge(e, false);
        }

        Collections.sort(orderedMostFrequentEdges);
        for( int i = (orderedMostFrequentEdges.size()-1); i >= 0; i-- ) {
            recoverableEdge = orderedMostFrequentEdges.get(i);
            if( recoverableEdge.getFrequency() > filterThreshold) this.addEdge(recoverableEdge);
            else {
                src = recoverableEdge.getSourceCode();
                tgt = recoverableEdge.getTargetCode();
                if( outgoings.get(src).isEmpty() || incomings.get(tgt).isEmpty() ) this.addEdge(recoverableEdge);
            }
        }
    }

    private void computeFilterThreshold() {
        ArrayList<DFGEdge> frequencyOrderedEdges = new ArrayList<>();
        int i;

        frequencyOrderedEdges.addAll(bestEdges);
//        if( percentileOnBest )
//        else frequencyOrderedEdges.addAll(edges);

        Collections.sort(frequencyOrderedEdges);
        i = (int)Math.round(frequencyOrderedEdges.size()*percentileFrequencyThreshold);
        if( i == frequencyOrderedEdges.size() ) i--;
        filterThreshold = frequencyOrderedEdges.get(i).getFrequency();
//        System.out.println("DEBUG - filter threshold: " + filterThreshold);
    }

    public void filterWithGuarantees() {
        bestEdgesOnMaxFrequencies();
        computeFilterThreshold();

        bestEdgesOnMaxCapacities();
        for( DFGEdge e : new HashSet<>(edges) )
            if( !bestEdges.contains(e) && !(e.getFrequency() >= filterThreshold) ) removeEdge(e, false);
    }

    private void bestEdgesOnMaxCapacities() {
        int src, tgt, cap, maxCap;
        boolean tiebreak;
        boolean useTiebreaker = false;
        DFGEdge bp, bs;

        LinkedList<Integer> toVisit = new LinkedList<>();
        Set<Integer> unvisited = new HashSet<>();

        HashMap<Integer, DFGEdge> bestPredecessorFromSource = new HashMap<>();
        HashMap<Integer, DFGEdge> bestSuccessorToSink = new HashMap<>();

        Map<Integer, Integer> maxCapacitiesFromSource = new HashMap<>();
        Map<Integer, Integer> maxCapacitiesToSink = new HashMap<>();

        for( int n : nodes.keySet() ) {
            maxCapacitiesFromSource.put(n, 0);
            maxCapacitiesToSink.put(n, 0);
        }

        maxCapacitiesFromSource.put(startcode, Integer.MAX_VALUE);
        maxCapacitiesToSink.put(endcode, Integer.MAX_VALUE);

//      forward exploration
        toVisit.add(startcode);
        unvisited.addAll(nodes.keySet());
        unvisited.remove(startcode);

        while( !toVisit.isEmpty() ) {
            src = toVisit.removeFirst();
            cap = maxCapacitiesFromSource.get(src);
            for( DFGEdge oe : outgoings.get(src) ) {
                tgt = oe.getTargetCode();
                maxCap = (cap > oe.getFrequency() ? oe.getFrequency() : cap);
                tiebreak = (maxCap == maxCapacitiesFromSource.get(tgt)) && bestPredecessorFromSource.get(tgt).isLoop() && useTiebreaker;
                if( (maxCap > maxCapacitiesFromSource.get(tgt)) || tiebreak ) {
                    maxCapacitiesFromSource.put(tgt, maxCap);
                    bestPredecessorFromSource.put(tgt, oe);
                    if( !toVisit.contains(tgt) ) unvisited.add(tgt);
                }
                if( unvisited.contains(tgt) ) {
                    toVisit.addLast(tgt);
                    unvisited.remove(tgt);
                }
            }
        }


//      backward exploration
        toVisit.add(endcode);
        unvisited.clear();
        unvisited.addAll(nodes.keySet());
        unvisited.remove(endcode);

        while( !toVisit.isEmpty() ) {
            tgt = toVisit.removeFirst();
            cap = maxCapacitiesToSink.get(tgt);
            for( DFGEdge ie : incomings.get(tgt) ) {
                src = ie.getSourceCode();
                maxCap = (cap > ie.getFrequency() ? ie.getFrequency() : cap);
                tiebreak = (maxCap == maxCapacitiesToSink.get(src)) && bestSuccessorToSink.get(src).isLoop() && useTiebreaker;
                if( (maxCap > maxCapacitiesToSink.get(src)) || tiebreak ) {
                    maxCapacitiesToSink.put(src, maxCap);
                    bestSuccessorToSink.put(src, ie);
                    if( !toVisit.contains(src) ) unvisited.add(src);
                }
                if( unvisited.contains(src) ) {
                    toVisit.addLast(src);
                    unvisited.remove(src);
                }
            }
        }

        bestEdges = new HashSet<>();
        for( int n : nodes.keySet() ) {
            bestEdges.add(bestPredecessorFromSource.get(n));
            bestEdges.add(bestSuccessorToSink.get(n));
        }
        bestEdges.remove(null);

//        for( int n : nodes.keySet() ) {
//            System.out.println("DEBUG - " + n + " : [" + maxCapacitiesFromSource.get(n) + "][" + maxCapacitiesToSink.get(n) + "]");
//        }
    }

    private void exploreAndRemove() {
        int src, tgt;

        LinkedList<Integer> toVisit = new LinkedList<>();
        Set<Integer> unvisited = new HashSet<>();

//      forward exploration
        toVisit.add(startcode);
        unvisited.addAll(nodes.keySet());
        unvisited.remove(startcode);

        while( !toVisit.isEmpty() ) {
            src = toVisit.removeFirst();
            for( DFGEdge oe : outgoings.get(src) ) {
                tgt = oe.getTargetCode();
                if( unvisited.contains(tgt) ) {
                    toVisit.addLast(tgt);
                    unvisited.remove(tgt);
                }
            }
        }

        for(int n : unvisited) {
            System.out.println("DEBUG - fwd removed: " + nodes.get(n).print());
            removeNode(n);
        }

//      backward exploration
        toVisit.add(endcode);
        unvisited.clear();
        unvisited.addAll(nodes.keySet());
        unvisited.remove(endcode);

        while( !toVisit.isEmpty() ) {
            tgt = toVisit.removeFirst();
            for( DFGEdge oe : incomings.get(tgt) ) {
                src = oe.getSourceCode();
                if( unvisited.contains(src) ) {
                    toVisit.addLast(src);
                    unvisited.remove(src);
                }
            }
        }

        for(int n : unvisited) {
            System.out.println("DEBUG - bkw removed: " + nodes.get(n).print());
            removeNode(n);
        }
    }

    /* data objects management */

    private void addNode(DFGNode n) {
        int code = n.getCode();

        nodes.put(code, n);
        if( !incomings.containsKey(code) ) incomings.put(code, new HashSet<DFGEdge>());
        if( !outgoings.containsKey(code) ) outgoings.put(code, new HashSet<DFGEdge>());
        if( !dfgp.containsKey(code) ) dfgp.put(code, new HashMap<Integer, DFGEdge>());
    }

    private void removeNode(int code) {
        HashSet<DFGEdge> removable = new HashSet<>();
        nodes.remove(code);
        for( DFGEdge e : incomings.get(code) ) removable.add(e);
        for( DFGEdge e : outgoings.get(code) ) removable.add(e);
        for( DFGEdge e : removable ) removeEdge(e, false);
    }

    private void addEdge(DFGEdge e) {
        int src = e.getSourceCode();
        int tgt = e.getTargetCode();

        edges.add(e);
        incomings.get(tgt).add(e);
        outgoings.get(src).add(e);
        dfgp.get(src).put(tgt, e);

//        System.out.println("DEBUG - added edge: " + src + " -> " + tgt);
    }

    private boolean removeEdge(DFGEdge e, boolean safe) {
        int src = e.getSourceCode();
        int tgt = e.getTargetCode();
        if( untouchableEdges != null && untouchableEdges.contains(e) ) {
            System.out.println("DEBUG - this edge ensures connectedness! not removable!");
            return false;
        }
        if( safe && (incomings.get(tgt).size() == 1) || (outgoings.get(src).size() == 1) ) return false;
        incomings.get(tgt).remove(e);
        outgoings.get(src).remove(e);
        dfgp.get(src).remove(tgt);
        edges.remove(e);
        return true;
//        System.out.println("DEBUG - removed edge: " + src + " -> " + tgt);
    }

    public int enhance( Set<String> subtraces ) {
        int enhancement = 0;
        StringTokenizer trace;

        DFGNode node, prevNode;
        DFGEdge edge;

        int event, prevEvent;

        for( String t : subtraces ) {
            System.out.println("INFO - (dfgp) subtrace : " + t);
            trace = new StringTokenizer(t, ":");

            prevEvent = Integer.valueOf(trace.nextToken());
            prevNode = nodes.get(prevEvent);

            while( trace.hasMoreTokens() ) {

                event = Integer.valueOf(trace.nextToken());
                node = nodes.get(event);

                if( !dfgp.containsKey(prevEvent) || !dfgp.get(prevEvent).containsKey(event) ) {
                    edge = new DFGEdge(prevNode, node);
                    this.addEdge(edge);
                    enhancement++;
                }

                prevEvent = event;
                prevNode = node;
            }
        }

        return enhancement;
    }

    public int reduce( Set<String> subtraces ) {
        int reduction = 0;
        StringTokenizer trace;
        int event, prevEvent;

        for( String t : subtraces ) {
            trace = new StringTokenizer(t, ":");
            prevEvent = Integer.valueOf(trace.nextToken());

            while( trace.hasMoreTokens() ) {
                event = Integer.valueOf(trace.nextToken());
                if( dfgp.containsKey(prevEvent) && dfgp.get(prevEvent).containsKey(event) ) {
                    if(this.removeEdge(dfgp.get(prevEvent).get(event), false)) reduction++;
                }
                prevEvent = event;
            }
        }

//        detectParallelisms();
        return reduction;
    }

    /* DEBUG methods */

    public void printEdges(boolean includeL1) {
        String edge;
        Map<Integer, String> events = log.getEvents();

        for(DFGEdge e : edges) {
            edge = events.get(e.getSourceCode()) + " > " + events.get(e.getTargetCode()) + " [" + e.getFrequency() + "]";
            System.out.println("DEBUG - edge : " + edge);
        }

        for(int l1 : loopsL1) {
            System.out.println("DEBUG - edge : " + events.get(l1) + " > " + events.get(l1) + " [ " + loopsL1Freq.get(l1) + " ]");
        }
    }

    public void printNodes() {
        for( DFGNode n : nodes.values() )
            System.out.println("DEBUG - node : " + n.print());
    }

    public void printParallelisms() {
        System.out.println("DEBUG - printing parallelisms:");
        for( int A : parallelisms.keySet() ) {
            System.out.print("DEBUG - " + A + " || " );
            for( int B : parallelisms.get(A) ) System.out.print( B + ",");
            System.out.println();
        }
    }

//    EXPERIMENTAL

// this method is exactly the same of bestEdgesOnMaxCapacities
    private void bestEdgesOnMaxCapacitiesForConnectedness() {
        int src, tgt, cap, maxCap;
        DFGEdge bp, bs;

        LinkedList<Integer> toVisit = new LinkedList<>();
        Set<Integer> unvisited = new HashSet<>();

        HashMap<Integer, DFGEdge> bestPredecessorFromSource = new HashMap<>();
        HashMap<Integer, DFGEdge> bestSuccessorToSink = new HashMap<>();

        Map<Integer, Integer> maxCapacitiesFromSource = new HashMap<>();
        Map<Integer, Integer> maxCapacitiesToSink = new HashMap<>();

        for( int n : nodes.keySet() ) {
            maxCapacitiesFromSource.put(n, 0);
            maxCapacitiesToSink.put(n, 0);
        }

        maxCapacitiesFromSource.put(startcode, Integer.MAX_VALUE);
        maxCapacitiesToSink.put(endcode, Integer.MAX_VALUE);

//      forward exploration
        toVisit.add(startcode);
        unvisited.addAll(nodes.keySet());
        unvisited.remove(startcode);

        while( !toVisit.isEmpty() ) {
            src = toVisit.removeFirst();
            cap = maxCapacitiesFromSource.get(src);
            for( DFGEdge oe : outgoings.get(src) ) {
                tgt = oe.getTargetCode();
                maxCap = (cap > oe.getFrequency() ? oe.getFrequency() : cap);
                if( (maxCap > maxCapacitiesFromSource.get(tgt)) ) { //|| ((maxCap == maxCapacitiesFromSource.get(tgt)) && (bestPredecessorFromSource.get(tgt).getFrequency() < oe.getFrequency())) ) {
                    maxCapacitiesFromSource.put(tgt, maxCap);
                    bestPredecessorFromSource.put(tgt, oe);
                    if( !toVisit.contains(tgt) ) unvisited.add(tgt);
                }
                if( unvisited.contains(tgt) ) {
                    toVisit.addLast(tgt);
                    unvisited.remove(tgt);
                }
            }
        }


//      backward exploration
        toVisit.add(endcode);
        unvisited.clear();
        unvisited.addAll(nodes.keySet());
        unvisited.remove(endcode);

        while( !toVisit.isEmpty() ) {
            tgt = toVisit.removeFirst();
            cap = maxCapacitiesToSink.get(tgt);
            for( DFGEdge ie : incomings.get(tgt) ) {
                src = ie.getSourceCode();
                maxCap = (cap > ie.getFrequency() ? ie.getFrequency() : cap);
                if( (maxCap > maxCapacitiesToSink.get(src)) ) { //|| ((maxCap == maxCapacitiesToSink.get(src)) && (bestSuccessorToSink.get(src).getFrequency() < ie.getFrequency())) ) {
                    maxCapacitiesToSink.put(src, maxCap);
                    bestSuccessorToSink.put(src, ie);
                    if( !toVisit.contains(src) ) unvisited.add(src);
                }
                if( unvisited.contains(src) ) {
                    toVisit.addLast(src);
                    unvisited.remove(src);
                }
            }
        }

        untouchableEdges = new HashSet<>();
        for( int n : nodes.keySet() ) {
            untouchableEdges.add(bestPredecessorFromSource.get(n));
            untouchableEdges.add(bestSuccessorToSink.get(n));
        }
        untouchableEdges.remove(null);

//        for( int n : nodes.keySet() ) {
//            System.out.println("DEBUG - " + n + " : [" + maxCapacitiesFromSource.get(n) + "][" + maxCapacitiesToSink.get(n) + "]");
//        }
    }

    private boolean isConnected() {
        int src, tgt;

        LinkedList<Integer> toVisit = new LinkedList<>();
        Set<Integer> unvisited = new HashSet<>();

//      forward exploration
        toVisit.add(startcode);
        unvisited.addAll(nodes.keySet());
        unvisited.remove(startcode);

        while( !toVisit.isEmpty() ) {
            src = toVisit.removeFirst();
            for( DFGEdge oe : outgoings.get(src) ) {
                tgt = oe.getTargetCode();
                if( unvisited.contains(tgt) ) {
                    toVisit.addLast(tgt);
                    unvisited.remove(tgt);
                }
            }
        }

        if(!unvisited.isEmpty()) return false;

//      backward exploration
        toVisit.add(endcode);
        unvisited.clear();
        unvisited.addAll(nodes.keySet());
        unvisited.remove(endcode);

        while( !toVisit.isEmpty() ) {
            tgt = toVisit.removeFirst();
            for( DFGEdge oe : incomings.get(tgt) ) {
                src = oe.getSourceCode();
                if( unvisited.contains(src) ) {
                    toVisit.addLast(src);
                    unvisited.remove(src);
                }
            }
        }

        if(!unvisited.isEmpty()) return false;

        return true;
    }

    public void addLoops1() {
        for(int l1 : loopsL1) addEdge(new DFGEdge(nodes.get(l1),nodes.get(l1),1));
    }

}

class Matrix {
    int[] successors;
    int is;

    HashMap<BitSet, Integer> matrix;
    int totalFrequency;

    Matrix(int successors) {
        this.successors = new int[successors];
        matrix = new HashMap<>();
        is = 0;
        totalFrequency = 0;
    }

    void addSuccessor(int s) {
        successors[is] = s;
        is++;
    }

    void addBitset(BitSet combo, int frequency) {
        totalFrequency+= frequency;
        if(matrix.get(combo) == null) matrix.put(combo, frequency);
        else matrix.put(combo, matrix.get(combo)+frequency);
    }

    void prune(double threshold) {
        Set<BitSet> lowFrequency = new HashSet<>();
        double avgFrequency = (double)totalFrequency/matrix.size();

        for(BitSet bs : matrix.keySet())
            if( ((double)matrix.get(bs)/avgFrequency) < threshold) lowFrequency.add(bs);
        System.out.println("DEBUG - removing " + lowFrequency.size() + " low frequency observations");

        for(BitSet bs : lowFrequency) matrix.remove(bs);
    }

    int[] getSuccessors() { return successors; }
    int totalSuccessors() { return is; }
    int rows() { return matrix.keySet().size(); }

    Set<BitSet> getObservations() { return matrix.keySet(); }

    void print() {
        System.out.println("DEBUG - # successors: " + is);
        System.out.println("DEBUG - matrix size: " + matrix.keySet().size());
        System.out.println("DEBUG - matrix full @: " + (int)(Math.pow(2,is) - 1));
        for(BitSet combo : matrix.keySet()) {
            for (int s = 0; s < is; s++) {
                if (combo.get(s)) System.out.print("1");
                else System.out.print("0");
            }
            System.out.println();
        }
    }

}
