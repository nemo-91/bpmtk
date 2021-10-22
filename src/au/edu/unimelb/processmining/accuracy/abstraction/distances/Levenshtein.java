package au.edu.unimelb.processmining.accuracy.abstraction.distances;

public class Levenshtein {

    public static int stringDistance(CharSequence lhs, CharSequence rhs) {
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

    public static int arrayDistance(int[] lhs, int[] rhs) {
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

//        System.out.println("DEBUG - distance : " + distance[lhs.length][rhs.length]);
        return distance[lhs.length][rhs.length];
    }

    public static int unbalancedArrayDistance(int[] lhs, int[] rhs) {
        int[][] distance = new int[lhs.length + 1][rhs.length + 1];
        int weight = 0; //set this either at 1 or 0

        for (int i = 0; i <= lhs.length; i++)
            distance[i][0] = i;
        for (int j = 1; j <= rhs.length; j++)
            distance[0][j] = j*weight;

        for (int i = 1; i <= lhs.length; i++)
            for (int j = 1; j <= rhs.length; j++)
                distance[i][j] = Math.min(
                                    Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + weight ),
                                    ((lhs[i - 1] == rhs[j - 1]) ? distance[i - 1][j - 1] : Integer.MAX_VALUE));

        return distance[lhs.length][rhs.length];
    }

    public static int[] noSwapArrayDistance(int[] lhs, int[] rhs) {
        int[][] distance = new int[lhs.length + 1][rhs.length + 1];
        int[] result = new int[4];
        int i, j;

        for (i = 0; i <= lhs.length; i++) distance[i][0] = i;
        for (j = 1; j <= rhs.length; j++) distance[0][j] = j;

        for (i = 1; i <= lhs.length; i++)
            for (j = 1; j <= rhs.length; j++)
                distance[i][j] = Math.min(
                        Math.min( distance[i-1][j] + 1, distance[i][j-1] + 1 ),
                        ((lhs[i-1] == rhs[j-1]) ? distance[i-1][j-1] : Integer.MAX_VALUE));

        i--;
        j--;

//        the total matching cost
        result[0] = distance[i][j];

//        the total synchronous moves
        result[1] = 0;

//        the total moves on the left array
        result[2] = 0;

//        the total moves on the right array
        result[3] = 0;

        while( i != 0 && j != 0 ) {
            if( lhs[i-1] == rhs[j-1] ) {
//                we had a synchronous move
                result[1]++;
                i--;
                j--;
            } else if( distance[i][j] == (distance[i-1][j] + 1) ) {
//                we had a move on the left array (move on log)
                result[2]++;
                i--;
            } else if ( distance[i][j] == (distance[i][j-1] +1) ) {
//                we had a move on the right array (move on model)
                result[3]++;
                j--;
            } else {
                System.out.println("ERROR - stuck here forever alone.");
            }
        }

        result[2] += i;
        result[3] += j;

        return result;
    }
}
