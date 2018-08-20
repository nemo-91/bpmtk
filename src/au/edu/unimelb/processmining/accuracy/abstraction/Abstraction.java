package au.edu.unimelb.processmining.accuracy.abstraction;

/**
 * Created by Adriano on 26/01/18.
 */
public abstract class Abstraction {
    public abstract double minus(Abstraction m);
    public abstract double minusHUN(Abstraction a);
    public abstract double minusGRD(Abstraction a);
    public abstract void print();
    public abstract double density();
}
