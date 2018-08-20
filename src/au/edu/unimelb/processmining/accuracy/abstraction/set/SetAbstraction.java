package au.edu.unimelb.processmining.accuracy.abstraction.set;

import au.edu.unimelb.processmining.accuracy.abstraction.Abstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.Edge;
import au.edu.unimelb.processmining.accuracy.abstraction.Node;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Adriano on 26/01/18.
 */
public class SetAbstraction extends Abstraction {
    private Map<Edge, Double> edges;
    private Map<String, Double> nodes;

    public SetAbstraction() {
        edges = new HashMap<>();
        nodes = new HashMap<>();
    }

    public boolean addNode(String label) {
        if(nodes.containsKey(label)) return false;
        nodes.put(label, 0.0);
        return true;
    }

    public boolean addEdge(String src, String tgt, int label) {
        Edge e = new Edge(src, tgt, label);
        if( edges.containsKey(e) ) return false;
        edges.put(e, 0.0);
        return true;
    }

    public void addNode(String label, double frequency) {
        Double p;
        if( (p = nodes.get(label)) != null ) frequency += p;
        nodes.put(label, frequency);
    }

    public void addEdge(String src, String tgt, int label, double frequency) {
        Double p;
        Edge e = new Edge(src, tgt, label);
        if( (p = edges.get(e)) != null ) frequency += p;
        edges.put(e, frequency);
    }

    public double minus(Abstraction a) {
        double difference;
        Set<Edge> es;

        if( !(a instanceof SetAbstraction) ) return -1;
        SetAbstraction s = (SetAbstraction) a;

        es = new HashSet<>(edges.keySet());
        es.removeAll(s.edges.keySet());
//        System.out.println("DEBUG - " + es.size() + " " + edges.size() + " " + s.edges.size());
        difference = 1.0 - ((double)es.size()/edges.size());
        return difference;
    }

    public double minusGRD(Abstraction a) {
//        TODO
        return this.minus(a);
    }

    public double minusHUN(Abstraction a) {
//        TODO
        return this.minus(a);
    }

    public double density() {
        return (double)edges.size()/(double)(nodes.size()*nodes.size());
    }

    public void print() {
        for( Edge e : edges.keySet() ) System.out.println(e.print() + " * " + edges.get(e) );
        for( String n : nodes.keySet() ) System.out.println(n);
        System.out.println("INFO - edges: " + edges.size() + " nodes: " + nodes.size());
    }
}
