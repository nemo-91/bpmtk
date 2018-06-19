package de.drscc.automaton;

import java.util.Collections;
import java.util.Stack;

import de.drscc.automaton.Transition;

public class TarjanSCC {

    private boolean[] marked;        // marked[v] = has v been visited?
    private int[] id;                // id[v] = id of strong component containing v
    private int[] low;               // low[v] = low number of v
    private int pre;                 // preorder number counter
    private int count;               // number of strongly-connected components
    private Stack<Integer> stack;


    /**
     * Computes the strong components of the digraph {@code G}.
     * @param G the digraph
     */
    public TarjanSCC(Automaton G) {
        marked = new boolean[Collections.max(G.states().keySet())+1];
        stack = new Stack<Integer>();
        id = new int[Collections.max(G.states().keySet())+1]; 
        low = new int[Collections.max(G.states().keySet())+1];
        for (int v :  G.states().keySet()) 
        {
            if (!marked[v]) dfs(G, v);
        }

        // check that id[] gives strong components
        //assert check(G);
    }

    private void dfs(Automaton G, int v) { 
        marked[v] = true;
        low[v] = pre++;
        int min = low[v];
        stack.push(v);
        for (Transition tr : G.states().get(v).outgoingTransitions()) 
        {
        	if (!marked[tr.target().id()]) dfs(G, tr.target().id());
            if (low[tr.target().id()] < min) min = low[tr.target().id()];
        }
        if (min < low[v]) 
        {
            low[v] = min;
            return;
        }
        int w;
        do 
        {
            w = stack.pop();
            id[w] = count;
            low[w] = G.states().size();
        } while (w != v);
        count++;
    }


    /**
     * Returns the number of strong components.
     * @return the number of strong components
     */
    public int count() {
        return count;
    }


    /**
     * Are vertices {@code v} and {@code w} in the same strong component?
     * @param  v one vertex
     * @param  w the other vertex
     * @return {@code true} if vertices {@code v} and {@code w} are in the same
     *         strong component, and {@code false} otherwise
     * @throws IllegalArgumentException unless {@code 0 <= v < V}
     * @throws IllegalArgumentException unless {@code 0 <= w < V}
     */
    public boolean stronglyConnected(int v, int w) {
        validateVertex(v);
        validateVertex(w);
        return id[v] == id[w];
    }

    /**
     * Returns the component id of the strong component containing vertex {@code v}.
     * @param  v the vertex
     * @return the component id of the strong component containing vertex {@code v}
     * @throws IllegalArgumentException unless {@code 0 <= v < V}
     */
    public int id(int v) {
        validateVertex(v);
        return id[v];
    }

    // does the id[] array contain the strongly connected components?
//    private boolean check(Automaton G) {
//        TransitiveClosure tc = new TransitiveClosure(G);
//        for (int v = 0; v < G.V(); v++) {
//            for (int w = 0; w < G.V(); w++) {
//                if (stronglyConnected(v, w) != (tc.reachable(v, w) && tc.reachable(w, v)))
//                    return false;
//            }
//        }
//        return true;
//    }

    // throw an IllegalArgumentException unless {@code 0 <= v < V}
    private void validateVertex(int v) {
        int V = marked.length;
        if (v < 0 || v >= V)
            throw new IllegalArgumentException("vertex " + v + " is not between 0 and " + (V-1));
    }

    /**
     * Unit tests the {@code TarjanSCC} data type.
     *
     * @param args the command-line arguments
     */
//    public void main(String[] args) {
//        
//        Digraph G = new Digraph(in);
//        TarjanSCC scc = new TarjanSCC(G);
//
//        // number of connected components
//        int m = scc.count();
//        StdOut.println(m + " components");
//
//        // compute list of vertices in each strong component
//        Queue<Integer>[] components = (Queue<Integer>[]) new Queue[m];
//        for (int i = 0; i < m; i++) {
//            components[i] = new Queue<Integer>();
//        }
//        for (int v = 0; v < G.V(); v++) {
//            components[scc.id(v)].enqueue(v);
//        }
//
//        // print results
//        for (int i = 0; i < m; i++) {
//            for (int v : components[i]) {
//                StdOut.print(v + " ");
//            }
//            StdOut.println();
//        }
//
//    }

}
