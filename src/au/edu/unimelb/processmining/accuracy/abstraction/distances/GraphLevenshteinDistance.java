package au.edu.unimelb.processmining.accuracy.abstraction.distances;

import au.edu.unimelb.processmining.accuracy.abstraction.Edge;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.Subtrace;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

import static au.edu.unimelb.processmining.accuracy.abstraction.subtrace.SubtraceAbstraction.SCALE;

/**
 * Created by Adriano on 09/02/18.
 */
public class GraphLevenshteinDistance {

    public GraphLevenshteinDistance(){}


    public double averageDistance(Set<Subtrace> subtraces1, Set<Subtrace> subtraces2, int order) {
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
                distance = (double)computeLevenshteinArrayDistance(st1ia, st2ia)/(double) order;
                if( minDistances.get(st1) > distance ) minDistances.put(st1, distance);
            }
        }

        distance = 0.0;
        for( double d : minDistances.values() ) distance += d;

        return distance/size;
    }

    public double getSubtracesDistance(Set<Subtrace> subtraces1, Set<Subtrace> subtraces2, int order) {
        double[][] matrix;
        double distance = 0;
        boolean contained = subtraces2.size() > subtraces1.size();
        int size = Math.max(subtraces1.size(), subtraces2.size());
        int rows = subtraces1.size();
        int cols = subtraces2.size();
        int leftovers = Math.abs(subtraces1.size() - subtraces2.size());
        int[] st1ia, st2ia;

        matrix = new double[rows][cols];
        for(int i=0; i < rows; i++) for(int j=0; j < cols; j++) matrix[i][j] = 1.0;

        int r = 0;
        for( Subtrace st1 : subtraces1 ) {
            st1ia = st1.printIA();
            int c = 0;
            for( Subtrace st2 : subtraces2 ) {
                st2ia = st2.printIA();
                matrix[r][c] = (double)computeLevenshteinArrayDistance(st1ia, st2ia)/(double) order;
                c++;
            }
            r++;
        }

        System.out.print("DEBUG - starting HUN... ");
//        distance = HungarianAlgorithm.hgAlgorithm(matrix, "min");
//        if(contained) distance -= leftovers;

        HU2 hu2 = new HU2(matrix);
        int[] matches = hu2.execute();


        for(int i =0; i < rows; i++)
            if(matches[i] == -1) {
                System.out.print("-1 : ");
                distance += 1;
            }
            else {
                System.out.print(matrix[i][matches[i]] + " : ");
                distance += matrix[i][matches[i]];
            }


        System.out.println(" ... DONE! ");

        return (distance/(subtraces1.size()*SCALE))*SCALE;
    }


//    this should be edges1 - edges2, leftover of edges2 are okay.
    public double getDistance(Set<Edge> edges1, Set<Edge> edges2) {
        double[][] matrix;
        String src1, src2, tgt1, tgt2;
        double distance;
        double d, ds, dt;
        int ls1, lt1;
        int r, c;

        boolean contained = edges2.size() > edges1.size();
        int size = Math.max(edges1.size(), edges2.size());
        int leftovers = Math.abs(edges1.size() - edges2.size());
        double quicktest = (double)leftovers/(double)edges1.size();

        if( !contained && quicktest > 0.99 ) return quicktest;

//        System.out.println("DEBUG - edges1 " + edges1.size());
//        System.out.println("DEBUG - edges2 " + edges2.size());

        matrix = new double[size][size];
        for(int i=0; i < size; i++) for(int j=0; j < size; j++) matrix[i][j] = 1.0;

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
                ds = (double)computeLevenshteinDistance(src1, src2)/(double)Math.max(ls1, src2.length());
                dt = (double)computeLevenshteinDistance(tgt1, tgt2)/(double)Math.max(lt1, tgt2.length());
                d = (dt + ds)/2.0;
//                System.out.println("DEBUG - " + ds + " " + dt + " " + d);
                matrix[r][c] = d;
                c++;
            }
            r++;
        }

//        for (int i = 0; i < matrix.length; i++) {
//            for (int j = 0; j < matrix[i].length; j++) {
//                System.out.print(matrix[i][j] + " ");
//            }
//            System.out.println();
//        }

        distance = HungarianAlgorithm.hgAlgorithm(matrix, "min");
        if(contained) distance -= leftovers;

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
                ds = computeLevenshteinDistance(src1, src2)/Math.max(ls1, src2.length());
                dt = computeLevenshteinDistance(tgt1, tgt2)/Math.max(lt1, tgt2.length());
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

    private int computeLevenshteinDistance(CharSequence lhs, CharSequence rhs) {
        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];

        for (int i = 0; i <= lhs.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= rhs.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= lhs.length(); i++)
            for (int j = 1; j <= rhs.length(); j++)
                distance[i][j] = Math.min(
                        Math.min( distance[i - 1][j] + 1, distance[i][j - 1] + 1 ),
                        distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1));

        return distance[lhs.length()][rhs.length()];
    }

    private int computeLevenshteinArrayDistance(int[] lhs, int[] rhs) {
        int[][] distance = new int[lhs.length + 1][rhs.length + 1];

        for (int i = 0; i <= lhs.length; i++)
            distance[i][0] = i;
        for (int j = 1; j <= rhs.length; j++)
            distance[0][j] = j;

        for (int i = 1; i <= lhs.length; i++)
            for (int j = 1; j <= rhs.length; j++)
                distance[i][j] = Math.min(
                        Math.min( distance[i - 1][j] + 1, distance[i][j - 1] + 1 ),
                        distance[i - 1][j - 1] + ((lhs[i - 1] == rhs[j - 1]) ? 0 : 1));

        return distance[lhs.length][rhs.length];
    }


    private class Pair {
        int r, c;

        Pair(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }
}
