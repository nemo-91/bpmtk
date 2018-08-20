package au.edu.unimelb.processmining.accuracy.abstraction.markovian;

/**
 * This class works as a sliding window to keep track of the previous events
 * either recorded in the log or replayed in the model.
 * the order corresponds to the size of the memory for the previous events.
 *
 * Created by Adriano on 24/01/18.
 */
public class MarkovLabel {
    private Integer[] label;
    private int order;
    private int i;
//    private int last;

    public final static int INIT = 0;

    public MarkovLabel(int order) {
        this.order = order;
        label = new Integer[order];
        for(i = 0; i < order; i++) label[i] = INIT;
        i = 0;
//            last = Integer.MIN_VALUE;
    }

    public MarkovLabel(MarkovLabel label) {
        this.order = label.order;
        this.label = new Integer[order];
        for(i = 0; i < order; i++) this.label[i] = new Integer(label.label[i]);
        this.i = label.i;
//            this.last = label.last;
    }

    public MarkovLabel(MarkovLabel label, int next) {
        this.order = label.order;
        this.label = new Integer[order];
        for(i = 0; i < order; i++) this.label[i] = new Integer(label.label[i]);
        this.i = label.i;
//            this.last = label.last;
        add(next);
    }

    public void add(int next) {
//        if(last == next) return;
//            last = next;
        label[i%order] = next;
        i++;
        i = i%order;
    }

    public String print() {
        String l = ":";
        for(int j=0; j<order; j++) {
            l = l + label[i%order] + ":";
            i++;
        }
        i = i%order;
        return l;
    }

    @Override
    public int hashCode(){
        return print().hashCode();
    }


    @Override
    public boolean equals(Object o){
        if( o instanceof MarkovLabel )
            return print().equals(((MarkovLabel) o).print());

        return false;
    }
}
