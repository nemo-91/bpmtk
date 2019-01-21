package au.edu.unimelb.processmining.accuracy.abstraction.distances;

import au.edu.unimelb.processmining.accuracy.abstraction.Edge;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.Subtrace;

import java.util.*;


/**
 * Created by Adriano on 09/02/18.
 */
public class GraphEditDistance {

    public GraphEditDistance(){}


    public double averageDistance(Collection<Subtrace> subtraces1, Collection<Subtrace> subtraces2, int order) {
        Map<Subtrace, Double> minDistances;
        double distance;
        int size = subtraces1.size();
        int[] st1ia, st2ia;

        minDistances = new HashMap<>();

        for( Subtrace st1 : subtraces1 ) {
            st1ia = st1.printIA();
            minDistances.put(st1, 1.0);
            for( Subtrace st2 : subtraces2 ) {
                st2ia = st2.printIA();
                distance = (double) Levenshtein.arrayDistance(st1ia, st2ia)/(double) order;
                if( minDistances.get(st1) > distance ) minDistances.put(st1, distance);
            }
        }

        distance = 0.0;
        for( double d : minDistances.values() ) distance += d;

        return distance/size;
    }

    public double getUnbalancedSubtracesDistance(Collection<Subtrace> subtraces1, Collection<Subtrace> subtraces2, double order) {
        double[][] matrix;
        double[][] umatrix;
        double distance = 0;
        int rows = subtraces1.size();
        int cols = subtraces2.size();
        int[] st1ia, st2ia;

        matrix = new double[rows][cols];
//        for(int i=0; i < rows; i++) for(int j=0; j < cols; j++) matrix[i][j] = 1.0;

        int r = 0;
        for( Subtrace st1 : subtraces1 ) {
            st1ia = st1.printIA();
            int c = 0;
            for( Subtrace st2 : subtraces2 ) {
                st2ia = st2.printIA();
                matrix[r][c] = (double)Levenshtein.unbalancedArrayDistance(st1ia, st2ia)/order;
                c++;
            }
            r++;
        }

        System.out.print("DEBUG - starting UHU... ");

        while( rows > 0 ) {
            HungarianAlgorithm hu2 = new HungarianAlgorithm(matrix);
            int[] matches = hu2.execute();

            int x = 0;
            if( rows - cols > 0) umatrix = new double[rows - cols][cols];
            else umatrix = null;

            for (int i = 0; i < rows; i++)
                if (matches[i] == -1) {
                    umatrix[x] = matrix[i];
                    x++;
//                    distance += 1;
                } else distance += matrix[i][matches[i]];

            matrix = umatrix;
            rows = rows - cols;
        }

        return distance/subtraces1.size();
    }

    public double getSubtracesDistance(Collection<Subtrace> subtraces1, Collection<Subtrace> subtraces2, double order) {
        double[][] matrix;
        double distance = 0;
        int rows = subtraces1.size();
        int cols = subtraces2.size();
        int[] st1ia, st2ia;

        matrix = new double[rows][cols];
        for(int i=0; i < rows; i++) for(int j=0; j < cols; j++) matrix[i][j] = 1.0;

        int r = 0;
        for( Subtrace st1 : subtraces1 ) {
            st1ia = st1.printIA();
            int c = 0;
            for( Subtrace st2 : subtraces2 ) {
                st2ia = st2.printIA();
                matrix[r][c] = (double)Levenshtein.arrayDistance(st1ia, st2ia)/order;
                c++;
            }
            r++;
        }

        HungarianAlgorithm hu2 = new HungarianAlgorithm(matrix);
        int[] matches = hu2.execute();

        for(int i =0; i < rows; i++)
            if(matches[i] == -1)  distance += 1;
            else distance += matrix[i][matches[i]];

        return distance/subtraces1.size();
    }

    public double getFreqWeightedSubtracesDistance(Collection<Subtrace> subtraces1, Collection<Subtrace> subtraces2, double order, double globalGramsCount) {
        double[][] matrix;
        double distance = 0;
        int rows = subtraces1.size();
        int cols = subtraces2.size();
        int[] st1ia, st2ia;

        matrix = new double[rows][cols];
//        for(int i=0; i < rows; i++) for(int j=0; j < cols; j++) matrix[i][j] = 1.0;

        int r = 0;
        for( Subtrace st1 : subtraces1 ) {
            st1ia = st1.printIA();
            int c = 0;
            for( Subtrace st2 : subtraces2 ) {
                st2ia = st2.printIA();
                matrix[r][c] = ((double)Levenshtein.arrayDistance(st1ia, st2ia)/order)*st1.getFrequency();
                c++;
            }
            r++;
        }

//        System.out.print("DEBUG - starting HUN... ");

        HungarianAlgorithm hu2 = new HungarianAlgorithm(matrix);
        int[] matches = hu2.execute();

        for(int i =0; i < rows; i++)
            if(matches[i] == -1)  distance += 1;
            else distance += matrix[i][matches[i]];

        return distance/globalGramsCount;
    }

//    this should be edges1 - edges2, leftover of edges2 are okay.
    public double getDistance(Set<Edge> edges1, Set<Edge> edges2) {
        double[][] matrix;
        String src1, src2, tgt1, tgt2;
        double distance = 0.0;
        double d, ds, dt;
        int ls1, lt1;
        int r, c;

        int rows = edges1.size();
        int cols = edges2.size();

        matrix = new double[rows][cols];
        for(int i=0; i < rows; i++) for(int j=0; j < cols; j++) matrix[i][j] = 1.0;

        r = 0;
        for( Edge e1 : edges1 ) {
            src1 = e1.getSRC();
            tgt1 = e1.getTGT();
            ls1 = src1.length();
            lt1 = tgt1.length();
            c = 0;
            for( Edge e2 : edges2 ) {
                src2 = e2.getSRC();
                tgt2 = e2.getTGT();
                ds = (double)Levenshtein.stringDistance(src1, src2)/(double)Math.max(ls1, src2.length());
                dt = (double)Levenshtein.stringDistance(tgt1, tgt2)/(double)Math.max(lt1, tgt2.length());
                d = (dt + ds)/2.0;
                matrix[r][c] = d;
                c++;
            }
            r++;
        }


        HungarianAlgorithm hu2 = new HungarianAlgorithm(matrix);
        int[] matches = hu2.execute();

        for(int i =0; i < rows; i++)
            if(matches[i] == -1)  distance += 1;
            else distance += matrix[i][matches[i]];

        return distance/edges1.size();
    }

    public double getDistanceGreedy(Set<Edge> iEdges1, Set<Edge> iEdges2) {
        HashMap<Double, ArrayList<Pair>> matrix;
        ArrayList<Edge> edges1 = new ArrayList<>(iEdges1);
        ArrayList<Edge> edges2 = new ArrayList<>(iEdges2);
        ArrayList<Double> distances;
        ArrayList<Pair> pairs;
        Pair pair;
        String src1, src2, tgt1, tgt2;
        double distance = 0.0;
        int leftovers;
        int s1, s2, ls1, lt1;
        double d, ds, dt;
        Collections.sort(edges1);
        Collections.sort(edges2);
        Set<Integer> removed1;
        Set<Integer> removed2;

        s1 = edges1.size();
        s2 = edges2.size();

        matrix = new HashMap<>();
        for( int i =0; i<s1; i++ ) {
            src1 = edges1.get(i).getSRC();
            tgt1 = edges1.get(i).getTGT();
            ls1 = src1.length();
            lt1 = tgt1.length();
            for (int j = 0; j < s2; j++) {
                src2 = edges2.get(j).getSRC();
                tgt2 = edges2.get(j).getTGT();
                ds = Levenshtein.stringDistance(src1, src2)/Math.max(ls1, src2.length());
                dt = Levenshtein.stringDistance(tgt1, tgt2)/Math.max(lt1, tgt2.length());
                d = (dt + ds)/2.0;
                if( !matrix.containsKey(d) ) matrix.put(d, new ArrayList<>());
                matrix.get(d).add(new Pair(i,j));
            }
        }

        removed1 = new HashSet<>();
        removed2 = new HashSet<>();
        distances = new ArrayList<>(matrix.keySet());
        Collections.sort(distances);

        s1 = distances.size();
        for( int i =0; i<s1; i++ ) {
            d = distances.get(i);
            pairs = matrix.get(d);
            s2 = pairs.size();
            for( int j =0; j<s2; j++) {
                pair = pairs.get(j);
                if( !removed1.contains(pair.r) && !removed2.contains(pair.c) ) {
                    distance += d;
                    removed1.add(pair.r);
                    removed2.add(pair.c);
                }
            }
        }

        leftovers = iEdges1.size() - removed1.size();

        distance = (distance + leftovers) / (double)iEdges1.size();
//        System.out.println("DEBUG - graph distance: " + distance);
        return distance;
    }

    private class Pair {
        int r, c;

        Pair(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }
}
