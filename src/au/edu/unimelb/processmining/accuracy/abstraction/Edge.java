package au.edu.unimelb.processmining.accuracy.abstraction;

import java.util.Objects;

/**
 * Created by Adriano on 23/01/18.
 */
public class Edge implements Comparable<Edge> {
    private String src, tgt;
    private String label;

    public Edge(String src, String tgt) {
        this.src = src;
        this.tgt = tgt;
        this.label = "";
    }

    public Edge(String src, String tgt, String label) {
        this.src = src;
        this.tgt = tgt;
        this.label = label;
    }

    public Edge(String src, String tgt, int label) {
        this.src = src;
        this.tgt = tgt;
        this.label = Integer.toString(label);
    }

    public String getLabel() { return label; }
    public String getSRC(){ return src; }
    public String getTGT(){ return tgt; }

    @Override
    public int hashCode() {
        return Objects.hash(src,tgt,label);
    }

    @Override
    public int compareTo(Edge e) {
        if( src.equals(e.src) ) {
            if( tgt.equals(e.tgt) ) return label.compareTo(e.label);
            else return tgt.compareTo(e.tgt);
        } else return src.compareTo(e.src);
    }

    @Override
    public boolean equals(Object o) {
        if( o instanceof Edge)
            return (src.equals(((Edge) o).src) && tgt.equals(((Edge) o).tgt)) && label.equals(((Edge) o).label);

        return false;
    }

    public String print() {
        return label + " ~ " + src + " -> " + tgt;
    }
}
