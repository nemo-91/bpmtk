package au.edu.unimelb.processmining.accuracy.abstraction.intermediate;

import static au.edu.unimelb.processmining.accuracy.abstraction.intermediate.AutomatonAbstraction.TAU;

/**
 * Created by Adriano on 24/01/18.
 */
public class AAEdge {
    private int id;
    protected int EID;
    protected AANode src, tgt;
    protected String label;

    public AAEdge(int id, AANode src, AANode tgt, int EID) {
        this.id = id;
        this.src = src;
        this.tgt = tgt;
        this.EID = EID;
    }

    public AAEdge(int id, AANode src, AANode tgt, int EID, String label) {
        this.id = id;
        this.src = src;
        this.tgt = tgt;
        this.EID = EID;
        this.label = (EID==TAU?"tau":label);
    }

    public int getEID() { return EID; }
    public AANode getSRC() { return src; }
    public AANode getTGT() { return tgt; }

    @Override
    public int hashCode() { return id; }

    @Override
    public boolean equals(Object o) {
        if( o instanceof AAEdge) return id == ((AAEdge) o).id;
        return false;
    }

    public String print() {
        return ("edge (" + label + "): " + src.getName() + "->" + tgt.getName());
    }
}
