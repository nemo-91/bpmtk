package au.edu.unimelb.processmining.accuracy.abstraction.subtrace;

import au.edu.qut.processmining.log.SimpleLog;
import au.edu.unimelb.processmining.accuracy.abstraction.Abstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.ProcessAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.distances.ConfusionMatrix;
import au.edu.unimelb.processmining.accuracy.abstraction.distances.GraphEditDistance;
import au.edu.unimelb.processmining.accuracy.abstraction.intermediate.AutomatonAbstraction;
import com.raffaeleconforti.conversion.bpmn.BPMNToPetriNetConverter;
import com.raffaeleconforti.conversion.petrinet.PetriNetToBPMNConverter;
import de.drscc.automaton.Automaton;
import de.drscc.importer.ImportProcessModel;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.*;

public class SubtraceAbstraction extends Abstraction {

    private Map<String, Subtrace> subtraces;
    private ArrayList<String> differences;
    private int order;
    private double globalGramsCount;

    private ConfusionMatrix matrix;
    Random random;

    public SubtraceAbstraction(int order) {
        this.order = order;
        subtraces = new HashMap<>();
        matrix = null;
        globalGramsCount = 0.0;
        random = new Random(1);
    }

    public void addSubtrace(Subtrace subtrace) {
        if( !subtrace.isPrintable() ) return;
        subtrace.frequency = 1.0;
        subtraces.put(subtrace.print(), subtrace);
    }

    public void addSubtrace(Subtrace subtrace, int frequency) {
        if( !subtrace.isPrintable() ) return;
        globalGramsCount+= (double)frequency;

        if( subtraces.containsKey(subtrace.print()) ) subtraces.get(subtrace.print()).frequency += frequency;
        else {
            subtrace.frequency = frequency;
            subtraces.put(subtrace.print(), subtrace);
        }
    }

    public double minus(Abstraction a) {
        double frequencyPenalty;
        Set<String> ast;

        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        ast = new HashSet<>(subtraces.keySet());
        ast.removeAll(m.subtraces.keySet());
//        System.out.println("DEBUG - " + ast.size() + " " + subtraces.size() + " " + m.subtraces.size());

        if( globalGramsCount == 0.0 )
            return 1.0 - (double)ast.size()/subtraces.size();

        frequencyPenalty = 0.0;
        for( String s : ast ) frequencyPenalty += subtraces.get(s).frequency;
        return 1.0 - frequencyPenalty/globalGramsCount;
    }

    public double minusHUN(Abstraction a) {
        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        GraphEditDistance gld = new GraphEditDistance();
        System.out.println("DEBUG - computing hungarian distance, global count: " + globalGramsCount);

        if( globalGramsCount == 0.0 )
            return 1.0 - gld.getSubtracesDistance(this.subtraces.values(), m.subtraces.values(), (double)order);

        return 1.0 - gld.getFreqWeightedSubtracesDistance(this.subtraces.values(), m.subtraces.values(), (double)order, globalGramsCount);
    }

    public double minusGRD(Abstraction a) {
        Set<Subtrace> leftovers;

        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        leftovers = new HashSet<>(this.subtraces.values());

        for( Subtrace st : m.subtraces.values() ) leftovers.remove(st);
        if( leftovers.isEmpty() ) return 1;

        GraphEditDistance gld = new GraphEditDistance();
        return 1.0 - gld.getSubtracesDistance(leftovers, m.subtraces.values(), (double)order);
    }

    public double minusUHU(Abstraction a) {
        if( !(a instanceof SubtraceAbstraction) ) return -1;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        GraphEditDistance gld = new GraphEditDistance();
//        System.out.println("DEBUG - computing hungarian distance... ");
        return 1.0 - gld.getUnbalancedSubtracesDistance(this.subtraces.values(), m.subtraces.values(), order);
    }

    public ConfusionMatrix confusionMatrix(Abstraction a) {
        if( !(a instanceof SubtraceAbstraction) ) return null;
        SubtraceAbstraction m = (SubtraceAbstraction) a;

        if(matrix != null) return matrix;

        matrix = new ConfusionMatrix(this, m);
        matrix.compute();
        return matrix;
    }

    public ArrayList<String> computeDifferences(SubtraceAbstraction sa) {
        HashSet<String> setdiff;
        ArrayList<Subtrace> sortedSubtraces;
        int next;

        setdiff = new HashSet<>(subtraces.keySet());
        setdiff.removeAll(sa.subtraces.keySet());

        differences = new ArrayList<>(setdiff);

//        if( globalGramsCount == 0.0 ) {
//            while( !sortedSubtraces.isEmpty() ) {
//                next = random.nextInt(sortedSubtraces.size());
//                differences.add(sortedSubtraces.remove(next).print());
//            }
//        } else {
//        sortedSubtraces = new ArrayList<>();
//        for( String st : setdiff ) sortedSubtraces.add(subtraces.get(st));
//        Collections.sort(sortedSubtraces);
//            for(int i = 1; i <= sortedSubtraces.size(); i++) {
//                differences.add(sortedSubtraces.get(sortedSubtraces.size() - i).print());
////                sortedSubtraces.get(sortedSubtraces.size() - i).frequency *= 0.70;
//            }
//        }

        return differences;
    }

    public String nextMismatch() {
        if(differences == null || differences.isEmpty()) return null;
        return differences.remove(random.nextInt(differences.size()));
    }

    public Set<String> getDifferences(SubtraceAbstraction sa, int neighbours) {
        Set<String> differences;
        ArrayList<Subtrace> sortedSubtraces;
        int next;

        differences = new HashSet<>(subtraces.keySet());
        differences.removeAll(sa.subtraces.keySet());

        sortedSubtraces = new ArrayList<>();
        for( String st : differences ) sortedSubtraces.add(subtraces.get(st));
        Collections.sort(sortedSubtraces);

        differences.clear();
        neighbours = neighbours<sortedSubtraces.size() ? neighbours : sortedSubtraces.size();

        if( globalGramsCount == 0.0 ) {
            for(int i = 1; i <= neighbours; i++) {
                next = random.nextInt(sortedSubtraces.size());
                differences.add(sortedSubtraces.get(next).print());
            }
        } else {
            for(int i = 1; i <= neighbours; i++) {
                differences.add(sortedSubtraces.get(sortedSubtraces.size() - i).print());
//                sortedSubtraces.get(sortedSubtraces.size() - i).frequency *= 0.70;
            }
        }

        return differences;
    }

    public double density(){ return 1.0; }

    public Set<Subtrace> getSubtraces() { return new HashSet<>(subtraces.values()); }

    public void print() {
        for( String st : subtraces.keySet() ) System.out.println(st + "-" + subtraces.get(st).isComplete());
        System.out.println("INFO - total subtraces: " + subtraces.size());
    }

    public static SubtraceAbstraction abstractProcessBehaviour(BPMNDiagram diagram, int order, SimpleLog slog) {
        ImportProcessModel importer = new ImportProcessModel();

        Object[] objects = BPMNToPetriNetConverter.convert(diagram);
        if(objects[1] == null) objects[1] = PetriNetToBPMNConverter.guessInitialMarking((Petrinet) objects[0]);

//        if(objects[1] == null) objects[1] = MarkingDiscoverer.constructInitialMarking(context, (Petrinet) objects[0]);
//        else MarkingDiscoverer.createInitialMarkingConnection(context, (Petrinet) objects[0], (Marking) objects[1]);

        try {
            Automaton automaton = importer.createFSMfromPetrinet((Petrinet) objects[0], (Marking) objects[1], null, null);
            AutomatonAbstraction automatonAbstraction = new AutomatonAbstraction(automaton, slog);

            return (new ProcessAbstraction(automatonAbstraction)).subtrace(order);
        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("ERROR - impossible to parse the process object.");
            return null;
        }
    }

    public static SubtraceAbstraction abstractProcessBehaviour(Petrinet pnet, Marking iMarking, int order, SimpleLog slog) {
        ImportProcessModel importer = new ImportProcessModel();

        try {
            Automaton automaton = importer.createFSMfromPetrinet(pnet, iMarking, null, null);
            AutomatonAbstraction automatonAbstraction = new AutomatonAbstraction(automaton, slog);

            return (new ProcessAbstraction(automatonAbstraction)).subtrace(order);
        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("ERROR - impossible to parse the process object.");
            return null;
        }
    }

}
