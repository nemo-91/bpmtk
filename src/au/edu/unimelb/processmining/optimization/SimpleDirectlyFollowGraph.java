package au.edu.unimelb.processmining.optimization;

import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.splitminer.dfgp.DFGEdge;
import au.edu.qut.processmining.miners.splitminer.dfgp.DirectlyFollowGraphPlus;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramImpl;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;

import java.util.*;

public class SimpleDirectlyFollowGraph extends DirectlyFollowGraphPlus {

    private SimpleLog slog;
    private int startcode;
    private int endcode;

    private BitSet dfg;
    private Map<Integer, HashSet<Integer>> parallelisms;
    private Set<Integer> loopsL1;
    private Set<Integer> tabu;

    private int size;
    private Integer[] outgoings;
    private Integer[] incomings;

    public SimpleDirectlyFollowGraph(SimpleDirectlyFollowGraph sdfg) {
        this.slog = sdfg.slog;
        this.startcode = sdfg.startcode;
        this.endcode = sdfg.endcode;
        this.dfg = (BitSet) sdfg.dfg.clone();
        this.parallelisms = sdfg.parallelisms;
        this.loopsL1 = sdfg.loopsL1;
        this.size = sdfg.size;
        this.outgoings = sdfg.outgoings.clone();
        this.incomings = sdfg.incomings.clone();
        this.tabu = new HashSet<>(sdfg.tabu);
    }

    public SimpleDirectlyFollowGraph(DirectlyFollowGraphPlus directlyFollowGraphPlus) {
        this.parallelisms = directlyFollowGraphPlus.getParallelisms();
        this.loopsL1 = directlyFollowGraphPlus.getLoopsL1();
        this.startcode = directlyFollowGraphPlus.getStartcode();
        this.endcode = directlyFollowGraphPlus.getEndcode();
        this.slog = directlyFollowGraphPlus.getSimpleLog();

        size = directlyFollowGraphPlus.size();
        tabu = new HashSet<>();

        outgoings = new Integer[size];
        incomings = new Integer[size];
        for(int i = 0; i<size; i++) outgoings[i] = incomings[i] = 0;

//        this bit array represents a graph (the directly-follows)
//        the cell (i,j) is set to TRUE if there exists an edge in the DFG with srcID = i and tgtID = j
//        reminder: matrix[i][j] = array[i*size + j];
//        i = 0 is the source of the graph (i.e. no incoming edges)
//        i = size-1 is the sink of the graph (i.e. no outgoing edges)
        dfg = new BitSet(size*size);

        int src, tgt;
        for( DFGEdge e : directlyFollowGraphPlus.getEdges() ) {
            src = e.getSourceCode();
            tgt = (e.getTargetCode() == endcode ? size-1 : e.getTargetCode());

//        reminder: matrix[i][j] = array[i*size + j];
            outgoings[src]++;
            incomings[tgt]++;

            dfg.set(src*size + tgt);
//            tabu.add(src*size + tgt);
        }
    }

    @Override
    public SimpleLog getSimpleLog(){ return slog; }

    @Override
    public boolean areConcurrent(int A, int B) {
        return (parallelisms.containsKey(A) && parallelisms.get(A).contains(B));
    }

    @Override
    public int enhance( Set<String> subtraces ) {
        int enhancement = 0;
        StringTokenizer trace;
        int src, tgt;

        for( String t : subtraces ) {
            trace = new StringTokenizer(t, ":");
            src = Integer.valueOf(trace.nextToken());

            while( trace.hasMoreTokens() ) {
                tgt = Integer.valueOf(trace.nextToken());
                if( tgt == endcode ) tgt = size -1;
                if( !dfg.get(src*size + tgt) ) {
                    dfg.set(src*size + tgt);
                    outgoings[src]++;
                    incomings[tgt]++;
                    enhancement++;
                }
                src = tgt;
            }
        }
        return enhancement;
    }

    public String enhance( String subtrace, int strength ) {
        String leftover;
        int enhancement = 0;
        int src, tgt;

        StringTokenizer trace = new StringTokenizer(subtrace, ":");
        src = Integer.valueOf(trace.nextToken());

        while( trace.hasMoreTokens() && enhancement != strength) {
            tgt = Integer.valueOf(trace.nextToken());
            if( tgt == endcode ) tgt = size -1;
            if( !dfg.get(src*size + tgt) ) {
                dfg.set(src*size + tgt);
                outgoings[src]++;
                incomings[tgt]++;
                enhancement++;
            }
            src = tgt;
        }

        if( !trace.hasMoreTokens() ) return null;
        else {
            leftover =  ":" + src + ":";
            while( trace.hasMoreTokens() ) leftover = leftover + trace.nextToken() + ":";
        }

        return leftover;
    }

    @Override
    public int reduce( Set<String> subtraces ) {
        int reduction = 0;
        StringTokenizer trace;
        int src, tgt;

        for( String t : subtraces ) {
            trace = new StringTokenizer(t, ":");
            src = Integer.valueOf(trace.nextToken());

            while( trace.hasMoreTokens() ) {
                tgt = Integer.valueOf(trace.nextToken());
                if( tgt == endcode ) tgt = size -1;
                if( isRemovable(src, tgt) && dfg.get(src*size + tgt) ) {
                    dfg.clear(src*size + tgt);
                    outgoings[src]--;
                    incomings[tgt]--;
                    reduction++;
                }
                src = tgt;
            }
        }
        return reduction;
    }

    public String reduce( String subtrace, int strength ) {
        String leftover;
        int reduction = 0;
        int src, tgt;

        StringTokenizer trace = new StringTokenizer(subtrace, ":");
        src = Integer.valueOf(trace.nextToken());

        while( trace.hasMoreTokens() && reduction != strength ) {
            tgt = Integer.valueOf(trace.nextToken());
            if( tgt == endcode ) tgt = size -1;
            if( isRemovable(src, tgt) && dfg.get(src*size + tgt) ) {
                dfg.clear(src*size + tgt);
                outgoings[src]--;
                incomings[tgt]--;
                reduction++;
            }
            src = tgt;
        }

        if( !trace.hasMoreTokens() ) return null;
        else {
            leftover =  ":" + src + ":";
            while( trace.hasMoreTokens() ) leftover = leftover + trace.nextToken() + ":";
        }

        return leftover;
    }

    public void perturb(int strength) {
        Random random = new Random();
        int max = size*size;
        int next;
        int src, tgt;

        while( strength != 0 ) {
            next = random.nextInt(max);
            if( !dfg.get(next) ) {
                if( (next/size != (size-1)) && (next%size != 0) ) {
                    strength--;
                    dfg.set(next);
                }
            } else if( isRemovable(src=next/size, tgt=next%size) ) {
                outgoings[src]--;
                incomings[tgt]--;
                strength--;
            }
        }
    }

    private boolean isRemovable(int src, int tgt) {
        return outgoings[src] > 1 && incomings[tgt] > 1 && !tabu.contains(src*size+tgt) ;
    }

    @Override
    public BPMNDiagram convertIntoBPMNDiagram() {
        BPMNDiagram diagram = new BPMNDiagramImpl("eSDFG-diagram");
        HashMap<Integer, BPMNNode> mapping = new HashMap<>();
        BPMNNode node;
        BPMNNode src, tgt;

        node = diagram.addEvent("0", Event.EventType.START, Event.EventTrigger.NONE, Event.EventUse.CATCH, true, null);
        mapping.put(0, node);

        for( int taskID = 1; taskID < (size-1); taskID++ ) {
            node = diagram.addActivity( Integer.toString(taskID), loopsL1.contains(taskID), false, false, false, false);
            mapping.put(taskID, node);
        }

        node = diagram.addEvent("-1", Event.EventType.END, Event.EventTrigger.NONE, Event.EventUse.THROW, true, null);
        mapping.put(size-1, node);

        for( int srcID = 0; srcID < size; srcID++ ) {
            src = mapping.get(srcID);
            for( int tgtID = 0; tgtID < size; tgtID++ ) {
                tgt = mapping.get(tgtID);
                if( dfg.get(srcID*size + tgtID) ) diagram.addFlow(src, tgt, "");
            }
        }
//        System.out.println("INFO - returning a BPMN diagram from a bis-set.");
        return diagram;
    }

    @Override
    public int hashCode() {
        return dfg.hashCode();
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof SimpleDirectlyFollowGraph)
            return dfg.equals(((SimpleDirectlyFollowGraph) o).dfg);
        else return false;
    }

}
