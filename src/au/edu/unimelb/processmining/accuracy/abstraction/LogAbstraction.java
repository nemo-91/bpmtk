package au.edu.unimelb.processmining.accuracy.abstraction;

import au.edu.qut.processmining.log.SimpleLog;
import au.edu.unimelb.processmining.accuracy.abstraction.markovian.MarkovLabel;
import au.edu.unimelb.processmining.accuracy.abstraction.markovian.MarkovAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.set.SetAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.set.SetLabel;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.Subtrace;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.SubtraceAbstraction;

import java.util.Map;
import java.util.StringTokenizer;

import static au.edu.unimelb.processmining.accuracy.abstraction.subtrace.Subtrace.INIT;

/**
 * Created by Adriano on 23/01/18.
 */
public class LogAbstraction {

    public static MarkovAbstraction markovian(SimpleLog log, int order) {
        MarkovAbstraction abstraction = new MarkovAbstraction();
        Map<String, Integer> traces = log.getTraces();

        StringTokenizer trace;
        int traceFrequency;

        int event;
        MarkovLabel label;
        String src;
        String tgt;

        for( String t : traces.keySet() ) {
            trace = new StringTokenizer(t, "::");
            traceFrequency = traces.get(t);
//            System.out.println("DEBUG - (" + traceFrequency + ")trace: " + t);

//            consuming the start event (artificial, always 0)
            trace.nextToken();
            label = new MarkovLabel(order);

//            we read the next event of the trace until it is finished
//            we do no parse the final artificial event (that is -1)
            src = label.print();
            abstraction.addNode(src, traceFrequency);
            while( trace.hasMoreTokens() && ((event = Integer.valueOf(trace.nextToken())) != -1) ) {
                label.add(event);
                tgt = label.print();
//                System.out.println("DEBUG - from " + src);
//                System.out.println("DEBUG - to " + tgt);
                abstraction.addEdge(src, tgt, traceFrequency);
                src = label.print();
                abstraction.addNode(src, traceFrequency);
            }
        }
        return abstraction;
    }

    public static SetAbstraction set(SimpleLog log) {
        SetAbstraction abstraction = new SetAbstraction();
        Map<String, Integer> traces = log.getTraces();

        StringTokenizer trace;
        int traceFrequency;

        int event;
        SetLabel label;
        String src;
        String tgt;

        for( String t : traces.keySet() ) {
//            System.out.println("DEBUG - trace: " + t);
            trace = new StringTokenizer(t, "::");
            traceFrequency = traces.get(t);

//            consuming the start event (artificial, always 0)
            trace.nextToken();
            label = new SetLabel(log.getReverseMap().size()+1);

//            we read the next event of the trace until it is finished
//            we do no parse the final artificial event (that is -1)
            while( trace.hasMoreTokens() && ((event = Integer.valueOf(trace.nextToken())) != -1) ) {
//                the first node of the abstraction is the empty set
                src = label.print();
                abstraction.addNode(src, traceFrequency);
                label.add(event);
                tgt = label.print();
//                System.out.println("DEBUG - from " + src);
//                System.out.println("DEBUG - to " + tgt);
                abstraction.addEdge(src, tgt, event, traceFrequency);
            }
        }

        return abstraction;
    }

    public static SubtraceAbstraction subtrace(SimpleLog log, int order) {
        SubtraceAbstraction abstraction = new SubtraceAbstraction(order);
        Map<String, Integer> traces = log.getTraces();

        StringTokenizer trace;
        int traceFrequency;

        int event;
        Subtrace subtrace;

        for( String t : traces.keySet() ) {
            trace = new StringTokenizer(t, "::");
            traceFrequency = traces.get(t);
//            System.out.println("DEBUG - (" + traceFrequency + ")trace: " + t);

//            consuming the start event (artificial, always 0)
            trace.nextToken();
            subtrace = new Subtrace(order);

//            we read the next event of the trace until it is finished
//            we do no parse the final artificial event (that is -1)

            while( trace.hasMoreTokens() && ((event = Integer.valueOf(trace.nextToken())) != -1) ) {
                subtrace.add(event);
                abstraction.addSubtrace(new Subtrace(subtrace), traceFrequency);
            }

            subtrace.add(INIT);
            abstraction.addSubtrace(subtrace, traceFrequency);
        }

//        abstraction.powerup();
        return abstraction;
    }
}
