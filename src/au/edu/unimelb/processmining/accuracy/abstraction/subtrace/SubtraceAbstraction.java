package au.edu.unimelb.processmining.accuracy.abstraction.subtrace;

import au.edu.unimelb.processmining.accuracy.abstraction.Abstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.distances.GraphLevenshteinDistance;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SubtraceAbstraction extends Abstraction {

    private Map<String, Double> subtraces;

    public SubtraceAbstraction() {
        subtraces = new HashMap<>();
    }

    public boolean addSubtrace(String subtrace) {
        boolean recorded = subtraces.containsKey(subtrace);
        if( recorded ) return false;
        subtraces.put(subtrace, 0.0);
        return true;
    }

    public boolean addSubtrace(String subtrace, int frequency) {
        boolean recorded = subtraces.containsKey(subtrace);

        if( recorded ) {
            subtraces.put(subtrace, subtraces.get(subtrace)+frequency);
            return false;
        }

        subtraces.put(subtrace, (double) frequency);
        return true;
    }


    public double minus(Abstraction a) {
        double difference;
        Set<String> ast;

        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        ast = new HashSet<>(subtraces.keySet());
        ast.removeAll(m.subtraces.keySet());
//        System.out.println("DEBUG - " + ast.size() + " " + subtraces.size() + " " + m.subtraces.size());
        difference = 1.0 - ((double)ast.size()/subtraces.size());
        return difference;
    }

    public double minusHUN(Abstraction a) {
        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        GraphLevenshteinDistance gld = new GraphLevenshteinDistance();
        System.out.println("DEBUG - computing hungarian distance... ");
        return 1.0 - gld.getSubtracesDistance(this.subtraces.keySet(), m.subtraces.keySet());
    }

    public double minusGRD(Abstraction a) {
//        TODO
        return minusHUN(a);
    }

    public double density(){ return 1.0; }

    public Set<String> getSubtraces() { return subtraces.keySet(); }

    public void print() {
        for( String s : subtraces.keySet() ) System.out.println(s);
        System.out.println("INFO - total subtraces: " + subtraces.size());
    }

}
