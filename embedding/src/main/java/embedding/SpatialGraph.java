package embedding;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Build spatial adjacency graph.
 *
 * Created by hxw186 on 1/9/17.
 */
public class SpatialGraph extends LayeredGraph {

    public static int numSamples = 5_000_000;
    public static int numLayer = LayeredGraph.numLayer;

    public SpatialGraph() {
        super();
    }

    /**
     * Only keep the top k neighboring vertices.
     * This should be called after adding all edges, and
     * before initializing source vertex and alias table.
     * @param k
     */
    public void keepNearestKVertices(int k) {
        for (Vertex v : allVertices.values()) {
            v.edgesOut.sort((v1, v2) -> - Double.compare(v1.weight, v2.weight));
            v.edgesOut = v.edgesOut.subList(0, k);
            v.outDegree = v.edgesOut.stream().mapToDouble(a -> a.weight).sum();
        }
    }

    public static SpatialGraph constructGraph_tract() {
        Tracts trts = new Tracts();

        long t1 = System.currentTimeMillis();
        System.out.println("Start generating spatial graph ...");
        SpatialGraph g = new SpatialGraph();
        for (Tract src : trts.tracts.values()) {
            for (Tract dst : trts.tracts.values()) {
                double d = src.distanceTo(dst);
                double w = Math.exp(-d * 100);
                g.addEdge(Integer.toString(src.id), Integer.toString(dst.id), w);
            }
        }

        /**
         * Only keep the nearest 10 tracts to address issue (#4)
         */
        g.keepNearestKVertices(10);

        g.sourceVertices = new LinkedList<>(g.allVertices.values());
        g.sourceWeightSum = g.sourceVertices.stream().mapToDouble(x -> x.outDegree).sum();
        g.initiateAliasTables();
        long t2 = System.currentTimeMillis();
        System.out.format("Spatial graph built successfully in %d milliseconds.\n", t2-t1);
        return g;
    }


    public static SpatialGraph constructGraph_CA() {
        CommunityAreas cas = new CommunityAreas();

        long t1 = System.currentTimeMillis();
        System.out.println("Start generating spatial graph for communities ... ");
        SpatialGraph g = new SpatialGraph();
        for (CommunityArea src : cas.communities.values()) {
            for (CommunityArea dst : cas.communities.values()) {
                double d = src.distanceTo(dst);
                double w = Math.exp(-d * 100);
                g.addEdge(Integer.toString(src.id), Integer.toString(dst.id), w);
            }
        }

        g.keepNearestKVertices(10);

        g.sourceVertices = new LinkedList<>((g.allVertices.values()));
        g.sourceWeightSum = g.sourceVertices.stream().mapToDouble(v -> v.outDegree).sum();
        g.initiateAliasTables();

        long t2 = System.currentTimeMillis();
        System.out.format("Spatial graph for community built successfully in %d milliseconds.\n", t2-t1);
        return g;
    }


    public static void outputSampleSequence(String regionLevel) {
        LayeredGraph.numLayer = SpatialGraph.numLayer;
        SpatialGraph g;
        if (regionLevel.equals("tract"))
            g = constructGraph_tract();
        else // if (regionLevel.equals("CA"))
            g = constructGraph_CA();
        long t2 = System.currentTimeMillis();
        System.out.println("Starting sequence sampling...");
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/deepwalkseq-%s/taxi-spatial.seq",
                    regionLevel)));
            for (int i = 0; i < numSamples; i++) {
                List<String> seq = g.sampleVertexSequence();
                for (int j = 0; j < seq.size(); j++) {
                    String nodeName = String.format("%d-%s", j, seq.get(j));
                    seq.set(j, nodeName);
                }
                String line = String.join(" ", seq) + "\n";
                fout.write(line);
                if (i % (numSamples/10) == 0)
                    System.out.format("%d%% finished\n", i/ (numSamples/100));
            }
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long t3 = System.currentTimeMillis();
        System.out.format("Sampling %d sequences finished in %d seconds.\n", numSamples, (t3-t2)/1000);
    }

    public static void main(String[] argv) {
        numLayer = 24;
        numSamples = 80_000; // number of nodes * 1000
        outputSampleSequence("CA");
    }
}
