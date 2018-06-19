package au.edu.unimelb.processmining.accuracy.abstraction.intermediate;

import au.edu.qut.processmining.log.SimpleLog;
import au.edu.unimelb.processmining.accuracy.abstraction.markovian.MarkovLabel;
import au.edu.unimelb.processmining.accuracy.abstraction.set.SetLabel;
import de.drscc.automaton.Automaton;
import de.drscc.automaton.Transition;
import org.processmining.models.graphbased.directed.transitionsystem.State;
import org.processmining.models.graphbased.directed.transitionsystem.TransitionSystem;

import java.util.*;

/**
 * Created by Adriano on 24/01/18.
 */
public class AutomatonAbstraction {

    public static final int TAU = Integer.MIN_VALUE;

    private Map<Integer, Integer> idsMapping;

    private AANode source;
    private Set<AAEdge> edges;
    private Map<Integer, AANode> nodes;
    private Map<Integer, Set<AAEdge>> outgoings;
//    private Map<Integer, Set<AMEdge>> incomings;
//    private Map<Integer, Map<Integer, AMEdge>> abstraction;


    public AutomatonAbstraction(Automaton automaton, SimpleLog log) {
        edges = new HashSet<>();
        nodes = new HashMap<>();
        outgoings = new HashMap<>();
    //        incomings = new HashMap<>();
    //        abstraction = new HashMap<>();

    //        System.out.println("DEBUG - input: " + automaton.states().size() + ":" + automaton.transitions().size());

        matchIDs(automaton.eventLabels(), log.getReverseMap());
        populate(automaton, log.getEvents());
    }

    public AutomatonAbstraction(TransitionSystem transitionSystem, SimpleLog log) {
        edges = new HashSet<>();
        nodes = new HashMap<>();
        outgoings = new HashMap<>();

        abstractTransitionSystem(transitionSystem, log);
    }

    private void abstractTransitionSystem(TransitionSystem transitionSystem, SimpleLog log) {
        Map<State, AANode> mapping;
        Set<Integer> targets;
        Set<Integer> sources;
        State src, tgt;
        AANode asrc, atgt;
        AAEdge edge;
        int id = 1;
        int label;

        mapping = new HashMap<>();
        sources = new HashSet<>();
        targets = new HashSet<>();
        for(org.processmining.models.graphbased.directed.transitionsystem.Transition t : transitionSystem.getEdges()) {
            src = t.getSource();
            tgt = t.getTarget();

            if(!mapping.containsKey(src)) {
                asrc = new AANode(id);
                nodes.put(id,asrc);
                mapping.put(src, asrc);
                outgoings.put(id, new HashSet<>());
                sources.add(id);
                id++;
            } else {
                asrc = mapping.get(src);
                sources.add(asrc.getID());
            }

            if(!mapping.containsKey(tgt)) {
                atgt = new AANode(id);
                nodes.put(id,atgt);
                mapping.put(tgt, atgt);
                outgoings.put(id, new HashSet<>());
                targets.add(atgt.getID());
                id++;
            } else {
                atgt = mapping.get(tgt);
                targets.add(atgt.getID());
            }

            if( t.getLabel().equalsIgnoreCase("tau")) label = TAU;
            else label = log.getReverseMap().get(t.getLabel().replace("-complete", ""));

            edge = new AAEdge(id, asrc, atgt, label, t.getLabel());
            id++;

            edges.add(edge);
            outgoings.get(asrc.getID()).add(edge);
        }

        sources.removeAll(targets);
        if( sources.size() == 1 ) {
            for(int i : sources ) {
                source = nodes.get(i);
//                System.out.println("DEBUG - source is: " + i);
            }
        } else {
            System.out.println("WARNING - multiple sources found (" + sources.size() + ").");
            source = new AANode(id);
            nodes.put(id, source);
            outgoings.put(id, new HashSet<>());
            id++;
            for(int i : sources) {
                edge = new AAEdge(id,source, nodes.get(i), TAU);
                edges.add(edge);
                outgoings.get(source.getID()).add(edge);
                id++;
            }
        }

//        this.print();
    }

//    we need to match the ids of the automaton to those of the log, on a task-labels basis
    private void matchIDs(Map<Integer, String> automatonEIDs, Map<String, Integer> logEIDs) {
        idsMapping = new HashMap<>();

        int wid = -2;
        Integer lid;

        for( int aid : automatonEIDs.keySet() ) {
            if( automatonEIDs.get(aid).equalsIgnoreCase("tau") ) idsMapping.put(aid, TAU);
            else if( (lid = logEIDs.get(automatonEIDs.get(aid))) == null ) idsMapping.put(aid, wid--);
            else idsMapping.put(aid, lid);
        }
    }

    private void populate(Automaton automaton,  Map<Integer, String> eNames) {
        int id;
        AANode src, tgt;
        AAEdge edge;
        int eid;

        for( Transition t : automaton.transitions().values() ) {
            id = t.target().id();
            if( (tgt = nodes.get(id)) == null ) {
                tgt = new AANode(id);
                nodes.put(id, tgt);
                outgoings.put(id, new HashSet<>());
            }

            id = t.source().id();
            if( (src = nodes.get(id)) == null ) {
                src = new AANode(id);
                nodes.put(id, src);
                outgoings.put(id, new HashSet<>());
            }

            eid = idsMapping.get(t.eventID());
            edge = new AAEdge(t.id(), src, tgt, eid, eNames.get(eid));
            edges.add(edge);
//            System.out.println("DEBUG - adding: " + idsMapping.get(t.eventID()) + " "  + src + " -> " + tgt);
            outgoings.get(id).add(edge);
        }


        source = nodes.get(automaton.sourceID());
        if(Collections.min(nodes.keySet()) != 0) System.out.println("INFO - conversion exit code: " + Collections.min(nodes.keySet()));
//        this.print();
    }

    public void generateMarkovianLabels(int order) {
        MarkovLabel label = new MarkovLabel(order);
//        explore(source, label);
        exploreLinear(source,order);
    }

    public void generateSetLabels(int size) {
        SetLabel label = new SetLabel(size);
        explore(source, label);
    }

    private void explore(AANode node, MarkovLabel label) {
        if( !node.addLabel(label.print()) ) return; //this node already has this label, so we do not need to explore it further

        for( AAEdge e : outgoings.get(node.getID()) ) {
            if( e.getEID() == TAU ) explore(e.tgt, new MarkovLabel(label));
            else explore(e.tgt, new MarkovLabel(label, e.getEID()));
        }
    }

    private void exploreLinear(AANode source, int order) {
        ArrayList<AANode> toVisit = new ArrayList<>();
        Map<AANode, Set<String>> explored = new HashMap<>();
        Set<String> tmpExplored;
        String tmpLabel = (new MarkovLabel(order)).print();
        char d = ':';
        int i = 0;
        int MAXL = 1000000;

        System.out.print("DEBUG - generating labels...");
        toVisit.add(source);
        source.addLabel(tmpLabel);
        while( !toVisit.isEmpty() && i < MAXL) {
            source = toVisit.remove(0);
            if( !explored.containsKey(source) ) explored.put(source, new HashSet<>());
            tmpExplored = explored.get(source);
            for( String label : new HashSet<>(source.getLabels()) ) {
//                System.out.println("debug - label " + source.getID() + " - " + label);
                if( tmpExplored.contains(label) ) continue;
                else tmpExplored.add(label);

                for( AAEdge e : outgoings.get(source.getID()) ) {
                    if( e.getEID() != TAU ) {
                        tmpLabel = label.substring(2) + e.getEID() + d;
                        while(tmpLabel.charAt(0) != d) tmpLabel = tmpLabel.substring(1);
                    } else tmpLabel = new String(label);

//                    System.out.println("debug - tmplabel " + tmpLabel);
                    if( !e.tgt.addLabel(tmpLabel) ) continue;
                    i++;
//                    if(i%(MAXL/10)==0) System.out.println(" " + i/(MAXL/10) + "% ...");
                    toVisit.add(e.tgt);
                }
            }
        }

        System.out.println(" done at " + (double)i/(double)MAXL);
    }

    private void explore(AANode node, SetLabel label) {
        if( !node.addLabel(label.print()) ) return; //this node already has this label, so we do not need to explore it further

        for( AAEdge e : outgoings.get(node.getID()) ) {
            explore(e.tgt, new SetLabel(label, e.getEID()));
        }
    }

    public AANode getSource() { return source; }
    public Set<AANode> getNodes() { return new HashSet<>(nodes.values()); }
    public Set<AAEdge> getEdges() { return edges; }
    public Map<Integer, Set<AAEdge>> getOutgoings() { return outgoings; }

    public void print() {
        System.out.println("INFO - A-automaton (n,e): " + nodes.size() + "," + edges.size());
        for(AAEdge e : edges) System.out.println("INFO - " + e.print());
    }

}
