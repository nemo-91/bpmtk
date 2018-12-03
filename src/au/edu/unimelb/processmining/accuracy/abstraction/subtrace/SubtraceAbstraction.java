package au.edu.unimelb.processmining.accuracy.abstraction.subtrace;

import au.edu.unimelb.processmining.accuracy.abstraction.Abstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.distances.ConfusionMatrix;
import au.edu.unimelb.processmining.accuracy.abstraction.distances.GraphEditDistance;

import java.util.*;

public class SubtraceAbstraction extends Abstraction {

    private Map<Subtrace, Double> subtraces;
    private int order;
    private double globalGramsCount;

    private ConfusionMatrix matrix;

    public SubtraceAbstraction(int order) {
        this.order = order;
        subtraces = new HashMap<>();
        matrix = null;
        globalGramsCount = 0.0;
    }

    public boolean addSubtrace(Subtrace subtrace) {
        if( !subtrace.isPrintable() ) return false;
        boolean recorded = subtraces.containsKey(subtrace);
        if( !recorded ) subtraces.put(subtrace, 1.0);
        return recorded;
    }

    public boolean addSubtrace(Subtrace subtrace, int frequency) {
        if( !subtrace.isPrintable() ) return false;
        boolean recorded = subtraces.containsKey(subtrace);
        globalGramsCount+= (double)frequency;

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
        difference = 1.0 - (double)ast.size()/subtraces.size();
        return difference;
    }

    public double minusHUN(Abstraction a) {
        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        GraphEditDistance gld = new GraphEditDistance();
//        System.out.println("DEBUG - computing hungarian distance... ");

        if( globalGramsCount == 0.0 )
            return 1.0 - gld.getSubtracesDistance(this.subtraces.keySet(), m.subtraces.keySet(), (double)order);

        for( Subtrace st : subtraces.keySet() ) st.setFrequency(subtraces.get(st));
        return 1.0 - gld.getFreqWeightedSubtracesDistance(this.subtraces.keySet(), m.subtraces.keySet(), (double)order, globalGramsCount);
//        return 1.0 - gld.getSubtracesDistance(this.subtraces.keySet(), m.subtraces.keySet(), (double)order);

    }

    public double minusGRD(Abstraction a) {
        Set<Subtrace> leftovers;

        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        leftovers = new HashSet<>(this.subtraces.keySet());

        for( Subtrace st : m.subtraces.keySet() ) leftovers.remove(st);
        if( leftovers.isEmpty() ) return 1;

        GraphEditDistance gld = new GraphEditDistance();
        return 1.0 - gld.getSubtracesDistance(leftovers, m.subtraces.keySet(), (double)order);
    }

    public double minusUHU(Abstraction a) {
        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        GraphEditDistance gld = new GraphEditDistance();
//        System.out.println("DEBUG - computing hungarian distance... ");
        return 1.0 - gld.getUnbalancedSubtracesDistance(this.subtraces.keySet(), m.subtraces.keySet(), order);
    }

    public ConfusionMatrix confusionMatrix(Abstraction a) {
        if( !(a instanceof SubtraceAbstraction) ) return null;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        if(matrix != null) return matrix;

        matrix = new ConfusionMatrix(this, m);
        matrix.compute();
        return matrix;
    }

    public Set<Subtrace> removeAll(SubtraceAbstraction sa) {
        Set<Subtrace> ast;
        HashMap<Double, Set<Subtrace>> frequencies = new HashMap<>();
        ArrayList<Double> sortedFreqs;
        Subtrace mostFrequent = null;
        double f;

        ast = new HashSet<>(subtraces.keySet());
        ast.removeAll(sa.subtraces.keySet());

        f = 0.0;
        for(Subtrace st : ast)
            if( f < subtraces.get(st) ) {
                f = subtraces.get(st);
                mostFrequent = st;
            }

        ast.clear();
        ast.add(mostFrequent);
        subtraces.put(mostFrequent, 0.0);

//        for(Subtrace st : ast) {
//            f = subtraces.get(st);
//            if( !frequencies.containsKey(f) ) frequencies.put(f, new HashSet<>());
//            frequencies.get(f).add(st);
//        }

//        ast.clear();
//        sortedFreqs = new ArrayList<>(frequencies.keySet());
//        Collections.sort(sortedFreqs);
//        for( double d : sortedFreqs ) {
//            ast.addAll(frequencies.get(d));
//            if(ast.size() > 1) break;
//        }

        return ast;
    }

    public double density(){ return 1.0; }

    public Set<Subtrace> getSubtraces() { return subtraces.keySet(); }

    public void print() {
        for( Subtrace st : subtraces.keySet() ) System.out.println(st.print() + "-" + st.isComplete());
        System.out.println("INFO - total subtraces: " + subtraces.size());
    }

}
