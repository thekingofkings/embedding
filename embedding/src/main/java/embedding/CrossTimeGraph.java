package embedding;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Build a cross-time graph to capture the dynamic taxi flow.
 *
 * Created by hxw186 on 1/9/17.
 */
public class CrossTimeGraph extends LayeredGraph {

    public static int numLayer = 8;
    public static int numSamples = 50_000_000;

    public CrossTimeGraph() {
        super();
    }


    /**
     * Output the sample sequence ({@link CrossTimeGraph#numLayer} nodes each line)
     * For Deepwalk training.
     */
    public static void outputSampleSequence() {
        Tracts trts = new Tracts();
        trts.mapTripsIntoTracts();

        long t1 = System.currentTimeMillis();
        System.out.println("Start generating cross-time graph...");
        int timeStep = 24 / numLayer;
        LayeredGraph g = new LayeredGraph();
        for (int h = 0; h < numLayer; h += timeStep) {
            for (Tract src : trts.tracts.values()) {
                for (Tract dst : trts.tracts.values()) {
                    int w = src.getFlowTo(dst.id, h, h+timeStep-1);
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
        long t2 = System.currentTimeMillis();
        System.out.format("Cross-time graph built successfully in %d milliseconds.\nStarting sequence sampling...\n",
                t2-t1);

        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter("../miscs/taxi-crosstime.seq"));
            for (int i = 0; i < numSamples; i ++) {
                List<String> seq = g.sampleVertexSequence();
                String line = String.join(" ", seq);
                fout.write(line + "\n");
                if (i % (numSamples/10) == 0)
                    System.out.format("%d%% finished\n", i / (numSamples/100));
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
