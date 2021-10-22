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

package au.edu.qut.processmining.miners.splitminer;

import au.edu.qut.bpmn.helper.DiagramHandler;
import au.edu.qut.bpmn.helper.GatewayMap;
import au.edu.qut.bpmn.structuring.StructuringService;
import au.edu.qut.processmining.log.ComplexLog;
import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.splitminer.dfgp.DFGEdge;
import au.edu.qut.processmining.miners.splitminer.dfgp.DFGNode;
import au.edu.qut.processmining.miners.splitminer.dfgp.DirectlyFollowGraphPlus;
import au.edu.qut.processmining.miners.splitminer.oracle.Oracle;
import au.edu.qut.processmining.miners.splitminer.oracle.OracleItem;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import au.edu.qut.processmining.miners.splitminer.ui.miner.SplitMinerUIResult;

import au.edu.unimelb.processmining.optimization.SimpleDirectlyFollowGraph;
import de.hpi.bpt.graph.DirectedEdge;
import de.hpi.bpt.graph.DirectedGraph;
import de.hpi.bpt.graph.abs.IDirectedGraph;
import de.hpi.bpt.graph.algo.rpst.RPST;
import de.hpi.bpt.graph.algo.rpst.RPSTNode;
import de.hpi.bpt.graph.algo.tctree.TCType;
import de.hpi.bpt.hypergraph.abs.Vertex;

import de.hpi.bpt.process.petri.util.BisimilarityChecker;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramImpl;
import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.*;

import java.util.*;

/**
 * Created by Adriano on 24/10/2016.
 */
public class SplitMiner {

    private SimpleLog log;
    private DirectlyFollowGraphPlus dfgp;
    private BPMNDiagram bpmnDiagram;

    private boolean replaceIORs;
    private boolean removeLoopActivities;
    private SplitMinerUIResult.StructuringTime structuringTime;

    private int gateCounter;
    private HashMap<String, Gateway> candidateJoins;

    private Set<Gateway> bondsEntries;
    private Set<Gateway> rigidsEntries;

    public SplitMiner() {
        this.replaceIORs = true;
        this.removeLoopActivities = true;
        this.structuringTime = SplitMinerUIResult.StructuringTime.NONE;
    }

    public SplitMiner(boolean replaceIORs, boolean removeLoopActivities) {
        this.replaceIORs = replaceIORs;
        this.removeLoopActivities = removeLoopActivities;
        this.structuringTime = SplitMinerUIResult.StructuringTime.NONE;
    }

    public DirectlyFollowGraphPlus getDFGP() { return dfgp; }

    public BPMNDiagram getBPMNDiagram() { return bpmnDiagram; }

    public BPMNDiagram mineBPMNModel(XLog log, XEventClassifier xEventClassifier, double percentileFrequencyThreshold, double parallelismsThreshold,
                                     DFGPUIResult.FilterType filterType, boolean parallelismsFirst,
                                     boolean replaceIORs, boolean removeLoopActivities, SplitMinerUIResult.StructuringTime structuringTime)
    {
        this.replaceIORs = replaceIORs;
        this.removeLoopActivities = removeLoopActivities;
        this.structuringTime = structuringTime;

        this.log = (new LogParser()).getSimpleLog(log, xEventClassifier, 1.00);
//        this.log = LogParser.getComplexLog(log, xEventClassifier);

        generateDFGP(percentileFrequencyThreshold, parallelismsThreshold, filterType, parallelismsFirst);
        try {
            transformDFGPintoBPMN();
            if (structuringTime == SplitMinerUIResult.StructuringTime.POST) structure();
        } catch(Exception e) {
            System.out.println("ERROR - something went wrong translating DFG to BPMN, trying a second time");
            e.printStackTrace();
            try{
                dfgp = new DirectlyFollowGraphPlus(this.log, percentileFrequencyThreshold, parallelismsThreshold, filterType, parallelismsFirst);
                dfgp.buildSafeDFGP();
                transformDFGPintoBPMN();
                if (structuringTime == SplitMinerUIResult.StructuringTime.POST) structure();
            } catch ( Exception ee ) {
                System.out.println("ERROR - nothing to do, returning the bare DFGP");
                return dfgp.convertIntoBPMNDiagramWithOriginalLabels();
            }
        }

        return bpmnDiagram;
    }

    public BPMNDiagram mineBPMNModel(SimpleLog log, XEventClassifier xEventClassifier, double percentileFrequencyThreshold, double parallelismsThreshold,
                                     DFGPUIResult.FilterType filterType, boolean parallelismsFirst,
                                     boolean replaceIORs, boolean removeLoopActivities, SplitMinerUIResult.StructuringTime structuringTime)
    {
        this.replaceIORs = replaceIORs;
        this.removeLoopActivities = removeLoopActivities;
        this.structuringTime = structuringTime;

//        this.log = (new LogParser()).getSimpleLog(log, xEventClassifier, 1.00);
        this.log = log;

        generateDFGP(percentileFrequencyThreshold, parallelismsThreshold, filterType, parallelismsFirst);
        try {
            transformDFGPintoBPMN();
            if (structuringTime == SplitMinerUIResult.StructuringTime.POST) structure();
        } catch(Exception e) {
            System.out.println("ERROR - something went wrong translating DFG to BPMN, trying a second time");
            e.printStackTrace();
            try{
                dfgp = new DirectlyFollowGraphPlus(log, percentileFrequencyThreshold, parallelismsThreshold, filterType, parallelismsFirst);
                dfgp.buildSafeDFGP();
                transformDFGPintoBPMN();
                if (structuringTime == SplitMinerUIResult.StructuringTime.POST) structure();
            } catch ( Exception ee ) {
                System.out.println("ERROR - nothing to do, returning the bare DFGP");
                return dfgp.convertIntoBPMNDiagramWithOriginalLabels();
            }
        }

        return bpmnDiagram;
    }

    private void generateDFGP(double percentileFrequencyThreshold, double parallelismsThreshold, DFGPUIResult.FilterType filterType, boolean parallelismsFirst) {
        dfgp = new DirectlyFollowGraphPlus(log, percentileFrequencyThreshold, parallelismsThreshold, filterType, parallelismsFirst);
        dfgp.setOracle(!parallelismsFirst);
        dfgp.buildDFGP();
    }

    public BPMNDiagram discoverFromDFGP(DirectlyFollowGraphPlus idfgp) {
        this.log = idfgp.getSimpleLog();
        dfgp = idfgp;
        try {
            transformDFGPintoBPMN();
            if (structuringTime == SplitMinerUIResult.StructuringTime.POST) structure();
        } catch(Exception e) {
            System.out.println("ERROR - something went wrong translating DFG to BPMN");
            e.printStackTrace();
            return dfgp.convertIntoBPMNDiagramWithOriginalLabels();
        }
        return bpmnDiagram;
    }

    public BPMNDiagram discoverFromSDFG(SimpleDirectlyFollowGraph sdfg) throws Exception {
        this.log = sdfg.getSimpleLog();
        dfgp = sdfg;
        transformDFGPintoBPMN();
        if (structuringTime == SplitMinerUIResult.StructuringTime.POST) structure();
        return bpmnDiagram;
    }

    private void transformDFGPintoBPMN() {
        DiagramHandler helper = new DiagramHandler();
        BPMNNode entry = null;
        BPMNNode exit = null;

//        System.out.println("SplitMiner - generating bpmn diagram");

        gateCounter = Integer.MIN_VALUE;

//        we retrieve the starting BPMN diagram from the DFGP,
//        it is a DFGP with start and end events, but no gateways
        bpmnDiagram = dfgp.convertIntoBPMNDiagram();
        candidateJoins = new HashMap<>();

//        there are only two events in the initial BPMN diagram,
//        one is the START and by exclusion the second is the END
        for( Event e : bpmnDiagram.getEvents() )
            if( e.getEventType() == Event.EventType.START ) entry = e;
            else exit = e;

        if( entry == null || exit == null ) {
//            this should never happen
            System.out.println("ERROR - entry(" + entry + ") OR exit(" + exit + ") not found in the DFGP-diagram");
            return;
        }

//        we start the transformation of the DFGP into BPMN by generating the splits
//        generateBitmatrixSplits();
        generateSplits(entry, exit);

//        after generating the split hierarchy we should have only SPLITs,
//        however, it may happen that some JOINs are generated as well (due to shared future)
//        it is important that we do not leave any gateway that is both a SPLIT and a JOIN
        helper.removeJoinSplit(bpmnDiagram);

//        at this point, all the splits were generated, along with just a few joins
//        now we focus only on the joins. we use the RPST in order to place INCLUSIVE joins
//        which will be turned into AND or XOR joins later
//        System.out.println("SplitMiner - generating SESE joins ...");
        bondsEntries = new HashSet<>();
        rigidsEntries = new HashSet<>();
        while( generateSESEjoins() );

//        this second method adds the remaining joins, which were no entry neither exits of any RPST node
//        System.out.println("SplitMiner - generating inner joins ...");
        generateInnerJoins();

//        if( structuringTime == SplitMinerUIResult.StructuringTime.PRE ) structure();
//        helper.removeEmptyParallelFlows(bpmnDiagram);
        helper.fixSoundness(bpmnDiagram);

//        finally, we turn all the inclusive joins placed, into proper joins: ANDs or XORs
//        System.out.println("SplitMiner - turning inclusive joins ...");
        replaceIORs(helper);

        updateLabels(this.log.getEvents());

//            helper.collapseSplitGateways(bpmnDiagram);
//            helper.collapseJoinGateways(bpmnDiagram);

        if(removeLoopActivities) helper.removeLoopActivityMarkers(bpmnDiagram);

//        System.out.println("SplitMiner - bpmn diagram generated successfully");
    }

    private void generateSplits(BPMNNode entry, BPMNNode exit) {
        HashMap<Integer, BPMNNode> mapping = new HashMap<>();
        BPMNNode tgt;
        ArrayList<BPMNNode> toVisit = new ArrayList<>();
        HashSet<BPMNNode> visited = new HashSet<>();

        HashSet<Integer> successors;
        HashSet<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> removableEdges;

        Oracle oracle = new Oracle();
        OracleItem oracleItem;
        OracleItem finalOracleItem;
        HashSet<OracleItem> oracleItems;

//        we perform a breadth-first exploration of the DFGP-diagram
//        every time we find a node with multiple outgoing edges we stop
//        and we generate the corresponding hierarchy of gateways

        toVisit.add(0, entry);
        while( toVisit.size() != 0 ) {
            entry = toVisit.remove(0);
            visited.add(entry);
//            System.out.println("DEBUG - visiting: " + entry.getLabel());

            if( entry == exit ) continue;

            if( bpmnDiagram.getOutEdges(entry).size() > 1 ) {
//                entry is a node with multiple outgoing edges

                successors = new HashSet<>();
                removableEdges = new HashSet<>();
                for( BPMNEdge<? extends BPMNNode, ? extends BPMNNode> oe : bpmnDiagram.getOutEdges(entry) ) {
                    tgt = oe.getTarget();
//                    we remove all the outgoing edges, because we will restore them with the split gateways
                    removableEdges.add(oe);
                    successors.add(Integer.valueOf(tgt.getLabel()));
                    mapping.put(Integer.valueOf(tgt.getLabel()), tgt);
                    if( !toVisit.contains(tgt) && !visited.contains(tgt) ) toVisit.add(tgt);
                }

                for( BPMNEdge<? extends BPMNNode, ? extends BPMNNode> e : removableEdges ) bpmnDiagram.removeEdge(e);

//                to decide the hierarchy of the gateways we use an Oracle item
//                an Oracle item is a string of the type past|future
//                more info about this object in its own class
                oracleItems = new HashSet<>();
                for( int a : successors ) {
//                    we generate one Oracle item for each successor of the entry
//                    the successor will be the past
                    oracleItem = new OracleItem();
                    oracleItem.fillPast(a);

//                    then we fill its future with all the successors which are in a concurrency relationship with it
//                    if a successor is not concurrent, it means it will we exclusive or directly follow
//                    if exclusive we do not have to care about it
//                    if directly follow, it will be processed later
                    for( int b : successors )
                        if( (a !=  b) && (dfgp.areConcurrent(a, b)) ) oracleItem.fillFuture(b);

                    oracleItem.engrave();
                    oracleItems.add(oracleItem);
                }

                finalOracleItem = oracle.getFinalOracleItem(oracleItems);

//                the finalOracleItem is a matryoshka containing the info about the gateway hierarchy
//                the following method will explore inside-out this matryoshka and will place the gateways accordingly
//                the entry will be the last node to be linked to the outer gateway of the hierarchy
                generateSplitsHierarchy(entry, finalOracleItem, mapping);
            } else {
//                we save the only successor of the src
                tgt = ((new ArrayList<>(bpmnDiagram.getOutEdges(entry))).get(0)).getTarget();
                if( !toVisit.contains(tgt) && !visited.contains(tgt) ) toVisit.add(tgt);
            }
        }
    }

    private void generateSplitsHierarchy(BPMNNode entry, OracleItem nextOracleItem, Map<Integer, BPMNNode> mapping) {
//        first of all we retrieve the type of the gateway we should place
        Gateway.GatewayType type = nextOracleItem.getGateType();
        BPMNNode node;
        Integer nodeCode;
        Gateway gate;
        Gateway candidateJoin;

//        System.out.println("DEBUG - generating split from Oracle ~ [xor|and]: " + nextOracleItem + " ~ [" + nextOracleItem.getXorBrothers().size() + "|" + nextOracleItem.getAndBrothers().size() + "]");

        if( candidateJoins.containsKey(nextOracleItem.toString()) ) {
//            these are joins, they are created considering the fact they share the same future (finalOracleItem)
            candidateJoin = candidateJoins.get(nextOracleItem.toString());
//            System.out.println("DEBUG - found " + candidateJoin.getGatewayType() + " join for the Oracle item: " + nextOracleItem.toString());
            bpmnDiagram.addFlow(entry, candidateJoin, "");
            return;
        }

        if( type == null ) {
//            if the type was null, it means we reached a simple activity, so we can link the entry with the activity
            nodeCode = nextOracleItem.getNodeCode();
            if( nodeCode != null ) {
                node = mapping.get(nodeCode);
                bpmnDiagram.addFlow(entry, node, "");
            } else System.out.println("ERROR - found an oracle item without brother and more than one element in its past");
            return;
        }

        gate = bpmnDiagram.addGateway(Integer.toString(gateCounter++), type);
        bpmnDiagram.addFlow(entry, gate, "");
        for( OracleItem next : nextOracleItem.getXorBrothers() ) generateSplitsHierarchy(gate, next, mapping);
        for( OracleItem next : nextOracleItem.getAndBrothers() ) generateSplitsHierarchy(gate, next, mapping);

        candidateJoins.put(nextOracleItem.toString(), gate);
    }

/*
    private void generateBitmatrixSplits() {
//      METHOD-DEPENDENT DATA STRUCTURES

//        for each split task, we have a matrix of bits, each column being a combination of successors tasks that are executed
        HashMap<Integer, Matrix> splitMaps = new HashMap<>();
//        for each split task, we have an array storing the successors ids
//        we query the array to know the index of the successor to update accordingly the row of a matrix
        HashMap<Integer, ArrayList<Integer>> successors = new HashMap<>();

//        these two structures keep track of the mapping between integer successors codes and the BPMN nodes
//        as well as the incoming edge of each successor, this is fundamental to edit the BPMN diagram later
        HashMap<Integer, BPMNNode> successorsToNodes = new HashMap<>();
        HashMap<Integer, BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> successorsToEdges = new HashMap<>();

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
        int MAXD = 4;
        int skipcounter =0;
        int i;

        Map<String, Integer> traces = log.getTraces();

        int size;
        int TID; // this is the split task ID
        int SID; // tmp successors ID
        ArrayList<Integer> tmpSuccessors;
        for(BPMNNode n : bpmnDiagram.getNodes())
            if((size = bpmnDiagram.getOutEdges(n).size()) > 1) {
                TID = Integer.valueOf(n.getLabel());
                splitMaps.put(TID, new Matrix(size));

                tmpSuccessors = new ArrayList<>(size);
                for(BPMNEdge<? extends BPMNNode, ? extends BPMNNode> e : bpmnDiagram.getOutEdges(n)) {
                    SID = Integer.valueOf(e.getTarget().getLabel());
                    tmpSuccessors.add(SID);
                    splitMaps.get(TID).addSuccessor(SID);
                    successorsToNodes.put(SID, e.getTarget());
                    successorsToEdges.put(SID, e);
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
            generateSplitsHierarchyFromObservationMatrix(sti, splitMaps.get(sti), successorsToNodes, successorsToEdges);

        System.out.println("DEBUG - skipcounter = " + skipcounter);
    }

    private void generateSplitsHierarchyFromObservationMatrix(int split, Matrix matrix, HashMap<Integer, BPMNNode> successorsToNodes, HashMap<Integer, BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> successorsToEdges) {
        boolean print = true;

        System.out.println("DEBUG - Matrix of Split Task: " + log.getEvents().get(split) + " (" + split + ")");
//        matrix.print();
//        matrix.prune(0.30);

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
            System.out.println("DEBUG - transposed matrix");
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
        do {
            System.out.println("DEBUG - new round of discovery");
            for (int s1 : transposedMatrix.keySet()) {
                bs1 = transposedMatrix.get(s1);
                analysed.add(s1);
                for (int s2 : transposedMatrix.keySet()) {
                    if( analysed.contains(s2) ) continue;
                    bs2 = transposedMatrix.get(s2);
                    type = determineGateway(s1, s2, bs1, bs2, ROWS, skips);
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
                if(removableSuccessors.contains(p.getLeft()) || removableSuccessors.contains(p.getRight())) continue;
//                when we find an AND we can remove one of the two without any issues
                bs1 = transposedMatrix.get(p.getLeft());
                removableSuccessors.add(p.getLeft());
                removableSuccessors.add(p.getRight());
                bs0 = new BitSet();
                for(int i = 0; i<ROWS; i++) bs0.set(i, bs1.get(i));
                transposedMatrix.put(p.getLeft()*100, bs0);
                System.out.println("DEBUG - AND("+ p.getLeft() + "," + p.getRight() + ")");
            }

            if(ANDs.isEmpty()) {
//            then we check for XORs
                for (Pair<Integer, Integer> p : XORs) {
                    if(removableSuccessors.contains(p.getLeft()) || removableSuccessors.contains(p.getRight())) continue;
                    System.out.println("DEBUG - XOR("+ p.getLeft() + "," + p.getRight() + ")");
//                when we find an XOR....
                    bs1 = transposedMatrix.get(p.getLeft());
                    bs2 = transposedMatrix.get(p.getRight());
                    removableSuccessors.add(p.getLeft());
                    removableSuccessors.add(p.getRight());
                    bs0 = new BitSet();
                    for(int i = 0; i<ROWS; i++) bs0.set(i, bs1.get(i) || bs2.get(i));
                    transposedMatrix.put(p.getLeft()*100, bs0);
                }

                if (XORs.isEmpty()) {
//            then we consider ORs
                    for (Pair<Integer, Integer> p : ORs) {
                        if(removableSuccessors.contains(p.getLeft()) || removableSuccessors.contains(p.getRight())) continue;
                        System.out.println("DEBUG - OR("+ p.getLeft() + "," + p.getRight() + ")");
//                when we find an OR...
                        bs1 = transposedMatrix.get(p.getLeft());
                        bs2 = transposedMatrix.get(p.getRight());
                        removableSuccessors.add(p.getLeft());
                        removableSuccessors.add(p.getRight());
                        bs0 = new BitSet();
                        for(int i = 0; i<ROWS; i++) bs0.set(i, bs1.get(i) || bs2.get(i));
                        transposedMatrix.put(p.getLeft()*100, bs0);
                    }

                    if(ORs.isEmpty()) {
//                        finally we consider ANDs with skips
                        for (Pair<Integer, Integer> p : SANDs) {
                            if(removableSuccessors.contains(p.getLeft()) || removableSuccessors.contains(p.getRight())) continue;
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
                            transposedMatrix.put(p.getLeft()*100, bs0);
                        }
                    }
                }
            }

            for( int rs : removableSuccessors ) transposedMatrix.remove(rs);
            removableSuccessors.clear();
            analysed.clear();
            ANDs.clear();
            ORs.clear();
            XORs.clear();
            SANDs.clear();
        } while (transposedMatrix.size() > 1);

    }

    private Gate determineGateway(int s1, int s2, BitSet bs1, BitSet bs2, int size, Set<Integer> skips) {
        Gate type = Gate.OR;
        boolean safe = true;
        int mismatch = 0;
        int match = 0;

        Set<Pair<Boolean, Boolean>> observations = new HashSet<>();
        for(int i = 0; i<size; i++)
            observations.add(new ImmutablePair<>(bs1.get(i),bs2.get(i)));

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
            return Gate.SAND;
        }

        if(observations.contains(skipR) && observations.contains(skipNone)) {
            skips.add(s2);
            return Gate.SAND;
        }

        if(observations.contains(skipL) && observations.contains(skipR)) return Gate.XOR;
        else return Gate.OR;


    }
*/
    private boolean generateSESEjoins() {
        int counter = 0;
        HashSet<String> changed = new HashSet<>();

        try {
            HashMap<String, BPMNNode> nodes = new HashMap<>();
            HashMap<BPMNNode, Vertex> vertexes = new HashMap<BPMNNode, Vertex>();

            HashMap<String, Gateway.GatewayType> gates = new HashMap<String, Gateway.GatewayType>();
            ArrayList<RPSTNode> rpstBottomUpHierarchy = new ArrayList<RPSTNode>();
            HashSet<RPSTNode> loops = new HashSet<>();
            HashSet<RPSTNode> rigids = new HashSet<>();

            IDirectedGraph<DirectedEdge, Vertex> graph = new DirectedGraph();
            Vertex src;
            Vertex tgt;

            BPMNNode bpmnSRC;
            BPMNNode bpmnTGT;
            Gateway gate;

            String entry, exit, gatify, matchingGate, srcVertex;


//            we build the graph from the BPMN Diagram, the graph is necessary to generate the RPST
//            we build the graph from the BPMN Diagram, the graph is necessary to generate the RPST

            for( Flow f : bpmnDiagram.getFlows((Swimlane) null) ) {
                bpmnSRC = f.getSource();
                bpmnTGT = f.getTarget();
                if( !vertexes.containsKey(bpmnSRC) ) {
                    src = new Vertex(bpmnSRC.getLabel());  //this may not be anymore a unique number, but still a unique label
                    if( bpmnSRC instanceof Gateway ) gates.put(bpmnSRC.getLabel(), ((Gateway) bpmnSRC).getGatewayType());
                    vertexes.put(bpmnSRC, src);
                    nodes.put(bpmnSRC.getLabel(), bpmnSRC);
                } else src = vertexes.get(bpmnSRC);

                if( !vertexes.containsKey(bpmnTGT) ) {
                    tgt = new Vertex(bpmnTGT.getLabel());  //this may not be anymore a unique number, but still a unique label
                    if( bpmnTGT instanceof Gateway ) gates.put(bpmnTGT.getLabel(), ((Gateway) bpmnTGT).getGatewayType());
                    vertexes.put(bpmnTGT, tgt);
                    nodes.put(bpmnTGT.getLabel(), bpmnTGT);
                } else tgt = vertexes.get(bpmnTGT);

                graph.addEdge(src, tgt);
            }


//            we use the graph to get the RPST of it
            RPST rpst = new RPST(graph);

//            then, we explore the RPST top-down, but just to save its bottom-up structure
//            in particular, we will focus on rigids and bonds
            RPSTNode root = rpst.getRoot();
            LinkedList<RPSTNode> toAnalize = new LinkedList<RPSTNode>();
            toAnalize.addLast(root);
            while( toAnalize.size() != 0 ) {
                root = toAnalize.removeFirst();

                for( RPSTNode n : new HashSet<RPSTNode>(rpst.getChildren(root)) ) {
                    switch( n.getType() ) {
                        case T:
                            break;
                        case P:
                            toAnalize.addLast(n);
                            break;
                        case R:
                            rigids.add(n);
                        case B:
                            exit = n.getExit().getName();
                            if( !gates.containsKey(exit) ) {
//                                System.out.println("DEBUG - found a bond exit (" + exit + ") that is not a gateway");
                                rpstBottomUpHierarchy.add(0, n);
                            } else {
                                entry = n.getEntry().getName();
                                if( !gates.containsKey(entry) ) {
//                                    this is the case when an RPSTNode is a LOOP
//                                    System.out.println("DEBUG - found a bond entry (" + entry + ") that is not a gateway");
                                    rpstBottomUpHierarchy.add(0, n);
                                    loops.add(n);
                                } else {
                                    if( n.getType() == TCType.R ) rigidsEntries.add((Gateway)nodes.get(entry));
                                    else bondsEntries.add((Gateway)nodes.get(entry));
                                }
                            }
                            toAnalize.addLast(n);
                            break;
                        default:
                    }
                }
            }

//            System.out.println("DEBUG - starting analysing RPST node: " + rpstBottomUpHierarchy.size() );
            HashMap<String, BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> removableEdges;
            HashSet<String> toRemove;
            RPSTNode rpstNode;
            boolean isLoop;
            boolean isRigid;
            Gateway.GatewayType gType;

            while( !rpstBottomUpHierarchy.isEmpty() ) {
                rpstNode = rpstBottomUpHierarchy.remove(0);
                entry = rpstNode.getEntry().getName();
                exit = rpstNode.getExit().getName();
                isLoop = loops.contains(rpstNode);
                isRigid = rigids.contains(rpstNode);

//                we have to transform an activity-join into a gateway-join
//                but: if the RPST node is a loop, its entry is the join and its exit is the split,
//                     if not, it would be vice-versa.
                gatify = isLoop ? entry : exit;
                matchingGate = isLoop ? exit : entry;

//                if we are analysing a RIGID, we cannot match the join with the split
//                otherwise, if it is the case of a BOND, we can
                gType = isRigid ? Gateway.GatewayType.INCLUSIVE : gates.get(matchingGate);
                gType = isLoop ? Gateway.GatewayType.DATABASED : gType;
//                gType = Gateway.GatewayType.INCLUSIVE; // decomment this in case of debugging for incorrect gateways types

//                if we already turned this activity into a gateway, we cannot edit it anymore
//                we will go through it again (if needed) in the next call of this method
//                this explain the outer while loop, and the boolean value returned by this method
                if( changed.contains(gatify) ) continue;
                changed.add(gatify);

                removableEdges = new HashMap<>();
                toRemove = new HashSet<>();
                bpmnTGT = nodes.get(gatify);

//                we save all the incoming edges to the activity to be turned into gateway,
//                because they must be removed and substituted by edges to a split gateway
//                whether they are inside the RPST node graph
                for( BPMNEdge<? extends BPMNNode, ? extends BPMNNode> ie : bpmnDiagram.getInEdges(bpmnTGT) )
                    removableEdges.put(ie.getSource().getLabel(), ie);

                IDirectedGraph<DirectedEdge, Vertex> rpstNodeGraph = rpstNode.getFragment();
                for( Vertex v : rpstNodeGraph.getVertices() )
                    if( v.getName().equals(gatify) ) {
//                        at this point we have everything we need to update the BPMN diagram and place the join
                        gate = bpmnDiagram.addGateway(Integer.toString(gateCounter++), gType);
                        counter++;
                        bpmnDiagram.addFlow(gate, bpmnTGT, "");

                        for( de.hpi.bpt.graph.abs.AbstractDirectedEdge e : rpstNodeGraph.getEdgesWithTarget(v) ) {
                            srcVertex = e.getSource().getName();
                            toRemove.add(srcVertex);
                            bpmnSRC = nodes.get(srcVertex);
                            bpmnDiagram.addFlow(bpmnSRC, gate, "");
                        }

                        for( String label : removableEdges.keySet() ) {
                            if( toRemove.contains(label) ) bpmnDiagram.removeEdge(removableEdges.get(label));
                            else if(isLoop) {
//                                loops require this special treatment
//                                we must remove ALL the incoming edges
//                                also those which are outside the RPST node graph,
//                                this is due to the fact the join in a loop is the entry of the RPST node,
//                                so that its incoming edges will be both inside the RPST node graph and outside
//                                and consequentially we couldn't catch those which were outside
                                bpmnSRC = nodes.get(label);
                                bpmnDiagram.addFlow(bpmnSRC, gate, "");
                                bpmnDiagram.removeEdge(removableEdges.get(label));
                            }
                        }
                    }
            }
        } catch( Error e ) {
            e.printStackTrace(System.out);
            System.out.println("ERROR - impossible to generate split gateways");
            return false;
        }

//        System.out.println("DEBUG - SESE joins placed: " + counter );
        return !changed.isEmpty();
    }

    private void generateInnerJoins() {
        HashSet<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> removableEdges;
        Set<BPMNNode> nodes = new HashSet<>(bpmnDiagram.getNodes());
        Gateway gate;
        int counter = 0;

//        all the activities found with multiple incoming edges are turned into inclusive joins
//        these activities are inside RIGID fragments of the BPMN model
        for( BPMNNode n : nodes ) {
            removableEdges = new HashSet<>(bpmnDiagram.getInEdges(n));
            if( (removableEdges.size() <= 1) || (n instanceof Gateway) ) continue;
            gate = bpmnDiagram.addGateway(Integer.toString(gateCounter++), Gateway.GatewayType.INCLUSIVE);
            counter++;
            bpmnDiagram.addFlow(gate, n, "");
            for( BPMNEdge<? extends BPMNNode, ? extends BPMNNode> e : removableEdges ) {
                bpmnDiagram.removeEdge(e);
                bpmnDiagram.addFlow(e.getSource(), gate, "");
            }
        }
//        System.out.println("DEBUG - inner joins placed: " + counter );
    }

    private void replaceIORs(DiagramHandler helper) {
        bondsEntries.removeAll(rigidsEntries);
        GatewayMap gatemap = new GatewayMap(bondsEntries, replaceIORs);
//        System.out.println("DEBUG - doing the magic ...");
        if( gatemap.generateMap(bpmnDiagram) ) {
            gatemap.detectAndReplaceIORs();
            gatemap.checkANDLoops(true);
        } else System.out.println("ERROR - something went wrong initializing the gateway map");

        int counter;
        int[] potentialORs;
        int tgt1, tgt2;
        boolean ORs = false;
        int columns;
        if(!replaceIORs) {
            potentialORs = dfgp.getPotentialORs();
            columns = (int) Math.sqrt(potentialORs.length);
            for( Gateway g : bpmnDiagram.getGateways() ) {
                if( g.getGatewayType() == Gateway.GatewayType.PARALLEL && bpmnDiagram.getOutEdges(g).size() > 1 ) {
                    counter = 0;
                    for( BPMNEdge<? extends BPMNNode, ? extends BPMNNode> oe1 : bpmnDiagram.getOutEdges(g) )
                        for( BPMNEdge<? extends BPMNNode, ? extends BPMNNode> oe2 : bpmnDiagram.getOutEdges(g) )
                            if( (oe1.getTarget() == oe2.getTarget()) || (oe1.getTarget() instanceof Gateway) || (oe2.getTarget() instanceof Gateway) ) continue;
                            else {
                                tgt1 = Integer.valueOf(oe1.getTarget().getLabel());
                                tgt2 = Integer.valueOf(oe2.getTarget().getLabel());
                                if( potentialORs[(tgt1*columns) + tgt2] > 0 ) counter++;
                            }
                    if( counter > bpmnDiagram.getOutEdges(g).size() ) {
                        ORs = true;
                        g.setGatewayType(Gateway.GatewayType.INCLUSIVE);
                    }
                }
            }
            if(ORs) helper.matchORs(bpmnDiagram);
        }
    }

    private void structure() {
        StructuringService ss = new StructuringService();
        BPMNDiagram structureDiagram = ss.structureDiagram(bpmnDiagram);
        bpmnDiagram = structureDiagram;
    }

    private void updateLabels(Map<Integer, String> events) {
//        this method just replace the labels of the activities in the BPMN diagram,
//        that so far have been numbers (in order to speed up the computation complexity)
        DiagramHandler helper = new DiagramHandler();
        BPMNDiagram duplicateDiagram = new BPMNDiagramImpl(bpmnDiagram.getLabel());
        HashMap<BPMNNode, BPMNNode> originalToCopy = new HashMap<>();
        BPMNNode src, tgt;
        BPMNNode copy;
        String label;

        for( BPMNNode n : bpmnDiagram.getNodes() ) {
            if( n instanceof Activity ) label = events.get(Integer.valueOf(n.getLabel()));
            else label = "";
            copy = helper.copyNode(duplicateDiagram, n, label);
            if( copy != null ) originalToCopy.put(n, copy);
            else System.out.println("ERROR - diagram labels updating failed [1].");
        }

        for( Flow f : bpmnDiagram.getFlows() ) {
            src = originalToCopy.get(f.getSource());
            tgt = originalToCopy.get(f.getTarget());

            if( src != null && tgt != null ) duplicateDiagram.addFlow(src, tgt, "");
            else System.out.println("ERROR - diagram labels updating failed [2].");
        }
        bpmnDiagram = duplicateDiagram;
    }

}

