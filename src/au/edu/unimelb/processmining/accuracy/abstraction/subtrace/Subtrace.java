package au.edu.unimelb.processmining.accuracy.abstraction.subtrace;

public class Subtrace implements Comparable {
    private int[] label;
    private int order;
    private int i;
    private boolean full;
    private boolean complete;
    private String print;

    protected double frequency;

    public final static int INIT = 0;
    private final static int START = 0;

    public Subtrace(int order) {
        this.order = order;
        label = new int[order];
        for(i = 0; i < order; i++) label[i] = INIT;
        i = START;
        full = false;
        complete = false;
        print = null;
        frequency = 0;
    }

    public Subtrace(Subtrace label) {
        this.order = label.order;
        this.label = new int[order];
        for(i = 0; i < order; i++) this.label[i] = label.label[i];
        this.i = label.i;
        this.full = label.full;
        this.complete = label.complete;
        this.print = label.print;
        this.frequency = label.frequency;
    }

    public Subtrace(Subtrace label, int next) {
        this(label);
        add(next);
    }

    public void add(int next) {
        if( next == INIT ) {
            complete = true;
            return;
        } else print = null;

        label[i] = next;
        i++;
        if(i == order) {
            i = 0;
            full = true;
        }
    }

    public String print() {
        if( print == null ) {
            print = ":";
            if (full) {
                for (int j = 0; j < order; j++) {
                    print = print + label[i % order] + ":";
                    i++;
                }
                i = i % order;
            } else {
                if (complete) for (int j = 0; j < i; j++) print = print + label[j] + ":";
                else print = null;
            }
        }
        return print;
    }

    public String forcePrint() {
        if( print == null ) {
            print = ":";
            if(full) {
                for(int j=0; j<order; j++) {
                    print = print + label[i%order] + ":";
                    i++;
                }
                i = i%order;
            } else for(int j=0; j<i; j++) print = print + label[j] + ":";
        }
        return print;
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

    public double getFrequency(){ return frequency; }
    public void setFrequency(double f) { frequency = f; }

    public int getOrder() { return order; }

    public boolean isComplete() { return complete; }
    public boolean isPrintable() { return full || complete; }// && i != 0)); }

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


    @Override
    public int compareTo(Object o) {
        if( o instanceof Subtrace ) return (int)frequency - (int)((Subtrace) o).frequency;
        return -1;
    }
}
