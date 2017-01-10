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

    public static int numSamples = 50_000_000;
    public static int numLayer = LayeredGraph.numLayer;

    public SpatialGraph() {
        super();
    }

    public static void outputSampleSequence() {
        Tracts trts = new Tracts();

        long t1 = System.currentTimeMillis();
        System.out.println("Start generating spatial graph ...");
        int timeStep = 24 / numLayer;
        SpatialGraph g = new SpatialGraph();
        for (Tract src : trts.tracts.values()) {
            for (Tract dst : trts.tracts.values()) {
                double d = src.distanceTo(dst);
                double w = Math.exp(-d);
                g.addEdge(Integer.toString(src.id), Integer.toString(dst.id), w);
            }
        }

        g.sourceVertices = new LinkedList<>(g.allVertices.values());
        g.sourceWeightSum = g.sourceVertices.stream().mapToDouble(x -> x.outDegree).sum();
        g.initiateAliasTables();
        long t2 = System.currentTimeMillis();
        System.out.format("Spatial graph built successfully in %d milliseconds.\nStarting sequence sampling...\n",
                t2-t1);

        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter("../miscs/taxi-crosstime.seq", true));
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
        outputSampleSequence();
    }
}
