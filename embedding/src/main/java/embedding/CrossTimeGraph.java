package embedding;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Build cross-time graph and sample node sequence from it.
 *
 * Created by kok on 1/2/17.
 */
public class CrossTimeGraph {
    public List<Edge> allEdges;
    public Map<String, Vertex> allVertices;
    public List<Vertex> sourceVertices;
    private double sourceWeightSum;

    public CrossTimeGraph() {
        allEdges = new LinkedList<>();
        allVertices = new HashMap<>();
        sourceVertices = new LinkedList<>();
        sourceWeightSum = 0;
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

    public List<String> sampleVertexSequence() {
        double s = Vertex.rnd.nextDouble() * sourceWeightSum;
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


    /**
     * Output the sample sequence (24 nodes each line)
     * For Deepwalk training.
     */
    public static void outputSampleSequence() {
        Tracts trts = new Tracts();
        trts.mapTripsIntoTracts();

        CrossTimeGraph g = new CrossTimeGraph();
        for (int h = 0; h < 24; h++) {
            for (Tract src : trts.tracts.values()) {
                for (Tract dst : trts.tracts.values()) {
                    int w = src.getFlowTo(dst.id, h);
                    if (w > 0)
                        g.addEdge(String.format("%d-%d", h, src.id),
                                String.format("%d-%d", (h+1)%24, dst.id), w);
                }
            }
        }
        for (Tract src : trts.tracts.values()) {
            String srcn = String.format("%d-%d", 0, src.id);
            if (g.allVertices.containsKey(srcn))
                g.addSourceVertex(srcn);
        }
        System.out.println(g.sourceVertices.size());

        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter("../miscs/taxi-crosstime.seq"));
            for (int i = 0; i < 512_000_000; i ++) {
                List<String> seq = g.sampleVertexSequence();
                String line = String.join(" ", seq);
                fout.write(line + "\n");
                if (i % 5_120000 == 0)
                    System.out.format("%d%% finished", i / 5120000);
            }
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv) {
        outputSampleSequence();
    }
}


class Edge {
    public Vertex from;
    public Vertex to;
    public double weight;

    public Edge(Vertex f, Vertex t, double w) {
        from = t;
        to = t;
        weight = w;
    }
}
class Vertex {
    public static Random rnd = new Random();

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