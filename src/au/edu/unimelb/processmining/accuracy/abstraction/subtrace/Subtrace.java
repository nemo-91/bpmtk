package au.edu.unimelb.processmining.accuracy.abstraction.subtrace;

public class Subtrace {
    private int[] label;
    private int order;
    private int i;
    private boolean full;
    private boolean complete;

    public final static int INIT = 0;
    private final static int START = 0;

    public Subtrace(int order) {
        this.order = order;
        label = new int[order];
        for(i = 0; i < order; i++) label[i] = INIT;
        i = START;
        full = false;
        complete = false;
    }

    public Subtrace(Subtrace label) {
        this.order = label.order;
        this.label = new int[order];
        for(i = 0; i < order; i++) this.label[i] = label.label[i];
        this.i = label.i;
        this.full = label.full;
        this.complete = label.complete;
    }

    public Subtrace(Subtrace label, int next) {
        this(label);
        add(next);
    }

    public Subtrace(Subtrace prev, Subtrace next) {
        order = prev.order+1;
        label = new int[order];
        for(int j = 0; j < prev.order; j++) this.label[j] = prev.label[(j+prev.i)%prev.order];
        label[order-1] = next.label[(next.i-1)%next.order];
        this.i = 0;
        full = true;
        complete = true;
    }

    public void add(int next) {
        if(next == INIT) {
            complete = true;
            return;
        }

        label[i] = next;
        i++;
        if(i == order) {
            i = 0;
            full = true;
        }
    }

    public String print() {
        String l = ":";

        if(full) {
            for(int j=0; j<order; j++) {
                l = l + label[i%order] + ":";
                i++;
            }
            i = i%order;
        } else {
            if( complete ) for(int j=0; j<i; j++) l = l + label[j] + ":";
            else return null;
        }

        return l;
    }

    public String forcePrint() {
        String l = ":";

        if(full) {
            for(int j=0; j<order; j++) {
                l = l + label[i%order] + ":";
                i++;
            }
            i = i%order;
        } else {
            for(int j=0; j<i; j++) l = l + label[j] + ":";
        }

        return l;
    }

    public int[] printIA() {
        int[] ia;

        if(full) {
            ia = new int[order];
            for(int j=0; j<order; j++) {
                ia[j] = label[i%order];
                i++;
            }
            i = i%order;
        } else {
            if( complete ) {
                ia = new int[i];
                for(int j=0; j<i; j++) ia[j] = label[j];
            } else return null;
        }

        return ia;
    }


    public boolean isComplete() { return complete; }
    public boolean isPrintable() { return (full || complete); }

    public boolean matches(Subtrace st) {
        int j = i+1;
        int k = st.i;
        int size = order -1;

        for( int z=0; z<size; z++ ) {
            if( label[j%order] != st.label[k%order] ) return false;
            j++;
            k++;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return forcePrint().hashCode();
    }


    @Override
    public boolean equals(Object o) {
        if( o instanceof Subtrace )
            return forcePrint().equals(((Subtrace) o).forcePrint());
        return false;
    }


}
