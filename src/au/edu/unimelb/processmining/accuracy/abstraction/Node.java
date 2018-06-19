package au.edu.unimelb.processmining.accuracy.abstraction;

/**
 * Created by Adriano on 23/01/18.
 */
public class Node {
    private String label;

    public Node(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    @Override
    public int hashCode() {
        return label.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if( o instanceof Node) return label.equals(((Node)o).label);
        return false;
    }
}
