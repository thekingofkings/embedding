package embedding;

import java.util.*;

/**
 * Build a layered graph and sample node sequence from it.
 * We use word2vec to learn the node embedding, and thus
 * each node name is String type.
 *
 * Created by kok on 1/2/17.
 */
public class LayeredGraph {

    public static Random rnd = new Random();
    public static int numLayer = 8;

    static public class Edge {
        public Vertex from;
        public Vertex to;
        public double weight;

        public Edge(Vertex f, Vertex t, double w) {
            from = f;
            to = t;
            weight = w;
        }
    }

    static public class Vertex {

        public String name;
        public int id;
        public List<Edge> edgesOut;
        public double outDegree;

        int[] aliasTable;
        double[] probTable;

        public Vertex(String n, int i) {
            name = n;
            id = i;
            edgesOut = new ArrayList<>();
            outDegree = 0;
        }

        public void addOutEdge(Edge e) {
            edgesOut.add(e);
            outDegree += e.weight;
        }

        /**
         * Preprocess to generate the alias table. This enables the O(1) random sampling.
         */
        public void initiateAliasTable() {
            int k = edgesOut.size();
            probTable = new double[k];
            aliasTable = new int[k];
            Arrays.fill(aliasTable, -1);

            for (int i = 0; i < k; i++) {
                double w = edgesOut.get(i).weight;
                probTable[i] = k * w / outDegree;
            }

            for (int l1 = 0; l1 < k; l1++) {
                if (probTable[l1] != 1.0 && aliasTable[l1] == -1) {
                    for (int l2 = 0; l2 < k; l2++) {
                        if (l2 != l1 && aliasTable[l2] == -1) {
                            if (probTable[l1] > 1.0 && probTable[l2] < 1.0) {
                                aliasTable[l2] = l1;
                                probTable[l1] -= 1 - probTable[l2];
                            } else if (probTable[l1] < 1.0 && probTable[l2] > 1.0) {
                                aliasTable[l1] = l2;
                                probTable[l2] -= 1 - probTable[l1];
                                // l1 is exactly full
                                break;
                            }
                        }
                    }
                }
            }
        }

        /**
         * O(V) sample next vertex
         * @deprecated this is slow, use the {@link Vertex#sampleNextVertex()} instead.
         * @return next vertex
         */
        public Vertex sampleNextVertex_OV() {
            double s = rnd.nextDouble() * outDegree;
            double cnt = 0;
            for (Edge e : edgesOut) {
                cnt += e.weight;
                if (cnt >= s)
                    return e.to;
            }
            return null;
        }

        /**
         * O(1) sample next vertex with alias table.
         * @return next Vertex
         */
        public Vertex sampleNextVertex() {
            int k = edgesOut.size();
            if (k == 0)
                return null;
            double x = rnd.nextDouble();
            int i = (int) (x * k);
            double y = x * k - i;

            if (y < probTable[i])
                return edgesOut.get(i).to;
            else
                return edgesOut.get(aliasTable[i]).to;
        }

        /**
         * O(1) sample next edge with alias table. [test purpose]
         * @param x the random number from range [0,1)
         * @return next vertex
         */
        public Vertex sampleNextVertex(double x) {
            int k = edgesOut.size();
            int i = (int) (x * k);
            double y = x * k - i;

            if (y < probTable[i])
                return edgesOut.get(i).to;
            else
                return edgesOut.get(aliasTable[i]).to;
        }
    }

    /**
     * ==================================================================
     * LayeredGraph starts here
     * ==================================================================
     */


    public List<Edge> allEdges;
    public Map<String, Vertex> allVertices;

    public List<Vertex> sourceVertices;
    protected double sourceWeightSum;
    protected double[] probTable;
    protected int[] aliasTable;

    public LayeredGraph() {
        allEdges = new ArrayList<>();
        allVertices = new HashMap<>();
        sourceVertices = new ArrayList<>();
        sourceWeightSum = 0;
    }

    public void addEdge(String fn, String tn, double weight) {
        Vertex f, t;
        if (! allVertices.containsKey(fn)) {
            f = new Vertex(fn, allVertices.size());
            allVertices.put(fn, f);
        } else
            f = allVertices.get(fn);

        if (! allVertices.containsKey(tn)) {
            t = new Vertex(tn, allVertices.size());
            allVertices.put(tn, t);
        } else
            t = allVertices.get(tn);

        Edge e = new Edge(f, t, weight);
        allEdges.add(e);
        f.addOutEdge(e);
    }

    /**
     * addSourceVertex should be called after all edges are added.
     * @param vn
     */
    public void addSourceVertex(String vn) {
        Vertex v;
        if (! allVertices.containsKey(vn))
            v = new Vertex(vn, allVertices.size());
        else
            v = allVertices.get(vn);

        sourceVertices.add(v);
        sourceWeightSum += v.outDegree;
    }

    /**
     * initialize alias tables for all vertices.
     * This should be called after all edges are added.
     */
    public void initiateAliasTables() {
        // initiate fast edge sampling
        allVertices.values().stream().forEach(v -> v.initiateAliasTable());
        // initiate fast source vertices sampling
        int k = sourceVertices.size();
        aliasTable = new int[k];
        probTable = new double[k];
        Arrays.fill(aliasTable, -1);

        for (int i = 0; i < k; i++) {
            double w = sourceVertices.get(i).outDegree;
            probTable[i] = k * w / sourceWeightSum;
        }

        for (int l1 = 0; l1 < k; l1++) {
            if (probTable[l1] != 1.0 && aliasTable[l1] == -1) {
                for (int l2 = 0; l2 < k; l2++) {
                    if (l2 != l1 && aliasTable[l2] == -1) {
                        if (probTable[l1] > 1.0 && probTable[l2] < 1.0) {
                            aliasTable[l2] = l1;
                            probTable[l1] -= 1 - probTable[l2];
                        } else if (probTable[l1] < 1.0 && probTable[l2] > 1.0) {
                            aliasTable[l1] = l2;
                            probTable[l2] -= 1 - probTable[l1];
                            // l1 is exactly full
                            break;
                        }
                    }
                }
            }
        }
    }

    public List<String> sampleVertexSequence() {
        LinkedList<String> seq = new LinkedList<>();
        double x = rnd.nextDouble();
        int k = sourceVertices.size();
        int i = (int) (x * k);
        double y = x * k - i;

        if ( y < probTable[i])
            seq.add(sourceVertices.get(i).name);
        else
            seq.add(sourceVertices.get(aliasTable[i]).name);

        while (seq.size() < numLayer) {
            Vertex v = allVertices.get(seq.getLast());
            Vertex nn = v.sampleNextVertex();
            if (nn == null)
                break;
            seq.add(nn.name);
        }
        return seq;
    }

}


