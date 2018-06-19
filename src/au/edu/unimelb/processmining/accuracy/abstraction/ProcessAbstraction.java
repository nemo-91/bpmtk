package au.edu.unimelb.processmining.accuracy.abstraction;

import au.edu.unimelb.processmining.accuracy.abstraction.intermediate.AAEdge;
import au.edu.unimelb.processmining.accuracy.abstraction.intermediate.AutomatonAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.markovian.MarkovAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.intermediate.AANode;
import au.edu.unimelb.processmining.accuracy.abstraction.set.SetAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.set.SetLabel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Created by Adriano on 23/01/18.
 */
public class ProcessAbstraction {

    public static MarkovAbstraction markovian(AutomatonAbstraction automatonAbstraction, int MAXE) {
        MarkovAbstraction abstraction = new MarkovAbstraction();
        AANode src;
        String tl;
        char d = ':';

        System.out.print("DEBUG - converting automaton to markov abstraction...");

        for( AAEdge e : automatonAbstraction.getEdges() ) {
            src = e.getSRC();
            if( e.getEID() == AutomatonAbstraction.TAU ) continue;
            for( String sl : src.getLabels() ) {
                tl = sl.substring(2) + e.getEID() + d;
                while( tl.charAt(0) != d ) tl = tl.substring(1);
                abstraction.addNode(sl);
                abstraction.addNode(tl);
                abstraction.addEdge(sl, tl);
            }
            if(abstraction.getEdges().size() > MAXE) break;
        }

        System.out.println(" done at " + (double)abstraction.getEdges().size()/(double)MAXE);
        return abstraction;
    }

        public static SetAbstraction set(AutomatonAbstraction automatonAbstraction) {
        SetAbstraction abstraction = new SetAbstraction();
        Map<Integer, Set<AAEdge>> outgoings = automatonAbstraction.getOutgoings();
        String tl;

        for( AANode src : automatonAbstraction.getNodes() )
            for( String sl : src.getLabels() ) {
                abstraction.addNode(sl);
                for( AAEdge e : outgoings.get(src.getID()) ) {
                    abstraction.addNode(sl);
                    tl = SetLabel.reverseAdd(sl, e.getEID());
                    abstraction.addEdge(sl, tl, e.getEID());
                }
            }

        return abstraction;
    }
}
