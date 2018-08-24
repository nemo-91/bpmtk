package au.edu.unimelb.processmining.accuracy.abstraction.subtrace;

public class Subtrace {
    private int[] label;
    private int order;
    private int i;
    private boolean complete;

    public final static int INIT = 0;

    public Subtrace(int order) {
        this.order = order;
        label = new int[order];
        for(i = 0; i < order; i++) label[i] = INIT;
        i = 1;
        complete = false;
    }

    public Subtrace(Subtrace label) {
        this.order = label.order;
        this.label = new int[order];
        for(i = 0; i < order; i++) this.label[i] = new Integer(label.label[i]);
        this.i = label.i;
        this.complete = label.complete;
    }

    public Subtrace(Subtrace label, int next) {
        this(label);
        add(next);
    }

    public void add(int next) {
        label[i] = next;
        i++;
        if(i == order) {
            i = 0;
            complete = true;
        }
    }

    public String print() {
        String l = ":";

        if(complete) {
            for(int j=0; j<order; j++) {
                l = l + label[i%order] + ":";
                i++;
            }
            i = i%order;
        } else {
            if( i==1 || label[i-1] != INIT ) return null;
            for(int j=0; j<i; j++) l = l + label[j] + ":";
        }

        return l;
    }

    public String forcePrint() {
        String l = ":";

        if(complete) {
            for(int j=0; j<order; j++) {
                l = l + label[i%order] + ":";
                i++;
            }
            i = i%order;
        } else for(int j=0; j<i; j++) l = l + label[j] + ":";

        return l;
    }

    public int[] printIA() {
        int[] ia;

        if(complete) {
            ia = new int[order];
            for(int j=0; j<order; j++) {
                ia[j] = label[i%order];
                i++;
            }
            i = i%order;
        } else {
            if( i==1 || label[i-1] != INIT ) return null;
            ia = new int[i];
            for(int j=0; j<i; j++) ia[j] = label[j];
        }

        return ia;
    }

    public boolean isComplete() {
        if( complete ) return true;
        else if( i==1 || label[i-1] != INIT ) return false;
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
