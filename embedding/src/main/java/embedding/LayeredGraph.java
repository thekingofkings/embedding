package embedding;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Build a layered graph and sample node sequence from it.
 * We use word2vec to learn the node embedding, and thus
 * each node name is String type.
 *
 * Created by kok on 1/2/17.
 */
public class LayeredGraph {

    public static Random rnd = new Random();

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

        public Vertex(String n, int i) {
            name = n;
            id = i;
            edgesOut = new LinkedList<>();
            outDegree = 0;
        }

        public void addOutEdge(Edge e) {
            edgesOut.add(e);
            outDegree += e.weight;
        }

        public Vertex sampleNextVertex() {
            double s = rnd.nextDouble() * outDegree;
            double cnt = 0;
            for (Edge e : edgesOut) {
                cnt += e.weight;
                if (cnt >= s)
                    return e.to;
            }
            return null;
        }
    }


    public List<Edge> allEdges;
    public Map<String, Vertex> allVertices;
    public List<Vertex> sourceVertices;
    protected double sourceWeightSum;

    public LayeredGraph() {
        allEdges = new LinkedList<>();
        allVertices = new HashMap<>();
        sourceVertices = new LinkedList<>();
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

    public List<String> sampleVertexSequence() {
        double s = rnd.nextDouble() * sourceWeightSum;
        LinkedList<String> seq = new LinkedList<>();

        double cnt = 0;
        for (Vertex v : sourceVertices) {
            cnt += v.outDegree;
            if (cnt >= s) {
                seq.add(v.name);
                break;
            }
        }
        while (seq.size() < 24) {
            Vertex v = allVertices.get(seq.getLast());
            Vertex nn = v.sampleNextVertex();
            if (nn == null)
                break;
            seq.add(nn.name);
        }
        return seq;
    }

}


