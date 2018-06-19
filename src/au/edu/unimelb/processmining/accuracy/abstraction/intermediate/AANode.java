package au.edu.unimelb.processmining.accuracy.abstraction.intermediate;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Adriano on 24/01/18.
 */
public class AANode {
    private int id;
    private Set<String> labels;
    private String name;

    public AANode(int id) {
        this.id = id;
        labels = new HashSet<>();
        name = null;
    }

    public AANode(int id, String name) {
        this.id = id;
        labels = new HashSet<>();
        this.name = name;
    }

    public boolean addLabel(String l) {
        if( labels.contains(l) ) return false;
        labels.add(l);
        return true;
    }

    public int getID() { return id; }
    public Set<String> getLabels() { return labels; }
    public String getName() { return name==null?Integer.toString(id):name; }

    @Override
    public int hashCode() { return id; }

    @Override
    public boolean equals(Object o) {
        if( o instanceof AANode) return id == ((AANode)o).id;
        return false;
    }
}
