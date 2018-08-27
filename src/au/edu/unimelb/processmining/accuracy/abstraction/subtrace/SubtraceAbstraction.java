package au.edu.unimelb.processmining.accuracy.abstraction.subtrace;

import au.edu.unimelb.processmining.accuracy.abstraction.Abstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.distances.GraphLevenshteinDistance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SubtraceAbstraction extends Abstraction {

    private Map<Subtrace, Double> subtraces;
    private int order;
    public static final int SCALE = 1;

    public SubtraceAbstraction(int order) {
        this.order = order;
        subtraces = new HashMap<>();
    }

    public boolean addSubtrace(Subtrace subtrace) {
        return this.addSubtrace(subtrace, 0);
    }

    public boolean addSubtrace(Subtrace subtrace, int frequency) {
        if( !subtrace.isPrintable() ) return false;
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
        Set<Subtrace> ast;

        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        ast = new HashSet<>(subtraces.keySet());
        ast.removeAll(m.subtraces.keySet());
//        System.out.println("DEBUG - " + ast.size() + " " + subtraces.size() + " " + m.subtraces.size());
        difference = 1.0 - (((double)ast.size()/(subtraces.size()*SCALE))*SCALE);
        return difference;
    }

    public double minusHUN(Abstraction a) {
        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        GraphLevenshteinDistance gld = new GraphLevenshteinDistance();
//        System.out.println("DEBUG - computing hungarian distance... ");
        return 1.0 - gld.getSubtracesDistance(this.subtraces.keySet(), m.subtraces.keySet(), order);
    }

    public double minusGRD(Abstraction a) {
        Set<Subtrace> leftovers;

        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        leftovers = new HashSet<>(this.subtraces.keySet());

        for( Subtrace st : m.subtraces.keySet() ) leftovers.remove(st);
        System.out.println("DEBUG - before : after - " + this.subtraces.size() + " : " + leftovers.size());

        if( leftovers.isEmpty() ) return 1;


        GraphLevenshteinDistance gld = new GraphLevenshteinDistance();
        return 1 - gld.getSubtracesDistance(leftovers, m.subtraces.keySet(), order);
    }

    public double density(){ return 1.0; }

    public Set<Subtrace> getSubtraces() { return subtraces.keySet(); }

    public void print() {
        for( Subtrace st : subtraces.keySet() ) System.out.println(st.print());
        System.out.println("INFO - total subtraces: " + subtraces.size());
    }

}
