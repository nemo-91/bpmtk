package au.edu.unimelb.processmining.accuracy.abstraction.set;

import java.util.BitSet;

/**
 * Created by Adriano on 26/01/18.
 */
public class SetLabel {
    private BitSet original;
    private BitSet extra;
    private int size;

    public SetLabel(int size) {
        this.size = size;
        original = new BitSet(size);
        extra = new BitSet(size);
    }

    public SetLabel(SetLabel label, int next) {
        size = label.size;
        original = (BitSet) label.original.clone();
        extra = (BitSet) label.extra.clone();
        add(next);
    }

    public void add(int next) {
        if( Math.abs(next) >= size ) {
            System.out.println("ERROR - impossible to update the label(" + size + ") correctly: " + next);
            return;
        }
        if( next < 0 ) extra.set(-next);
        else original.set(next);
    }

    public String print() {
        String o = "";
        String e = "";
        for(int i=0; i<size; i++) {
            o += original.get(i)?"1":"0";
            e += extra.get(i)?"1":"0";
        }
        return o + "-" + e;
    }

    public static String reverseAdd(String label, int next) {
        int size = label.length();
        int start = 0;
        String result = "";

        if( next < 0 ) {
            start = (size+1)/2;
            result += label.substring(0, start);
            next = -next;
        }
        for(int i=start; i<size; i++) {
            if(i==next) result += "1";
            else result += label.charAt(i);
        }

        return result;
    }

    @Override
    public int hashCode(){
        return print().hashCode();
    }


    @Override
    public boolean equals(Object o){
        if( o instanceof SetLabel )
            return print().equals(((SetLabel) o).print());

        return false;
    }
}
