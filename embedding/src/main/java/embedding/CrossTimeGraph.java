package embedding;

import com.vividsolutions.jts.awt.PointShapeFactory;

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

    public static int numSamples = 7_000_000;
    public static int numLayer = LayeredGraph.numLayer;

    public CrossTimeGraph() {
        super();
    }

    public static CrossTimeGraph constructGraph_tract() {
        Tracts trts = new Tracts();
        trts.mapTripsIntoTracts();

        long t1 = System.currentTimeMillis();
        System.out.println("Start generating cross-time graph...");
        int timeStep = 24 / numLayer;
        CrossTimeGraph g = new CrossTimeGraph();
        for (int h = 0; h < numLayer; h++) {
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
        System.out.format("Cross-time graph built successfully in %d milliseconds.\n", t2-t1);
        return g;
    }

    public static CrossTimeGraph constructGraph_CA() {
        CommunityAreas cas = new CommunityAreas();
        cas.mapTripsIntoCommunities();

        long t1 = System.currentTimeMillis();
        System.out.println("Start generating crosstime graph for Communities ...");
        int timeStemp = 24/ numLayer;
        CrossTimeGraph g = new CrossTimeGraph();
        for (int h = 0; h < numLayer; h++) {
            for (CommunityArea src : cas.communities.values()) {
                for (CommunityArea dst : cas.communities.values()) {
                    int w = src.getFlowTo(dst.id, h, h+timeStemp-1);
                    if (w > 0)
                        g.addEdge(String.format("%d-%d", h, src.id),
                                String.format("%d-%d", h, dst.id), w);
                }
            }
        }
        for (CommunityArea ca : cas.communities.values()) {
            String can = String.format("0-%d", ca.id);
            if (g.allVertices.containsKey(can))
                g.addSourceVertex(can);
        }

        long t2 = System.currentTimeMillis();
        System.out.format("Crosstime graph for communities built successfully in %d milliseconds.\n", t2-t1);
        return g;
    }


    /**
     * Output the sample sequence ({@link CrossTimeGraph#numLayer} nodes each line)
     * For Deepwalk training.
     */
    public static void outputSampleSequence(String regionLevel) {
        LayeredGraph.numLayer = SpatialGraph.numLayer;
        CrossTimeGraph g;
        if (regionLevel.equals("tract"))
            g = constructGraph_tract();
        else// if (regionLevel.equals("CA"))
            g = constructGraph_CA();
        g.initiateAliasTables();
        long t2 = System.currentTimeMillis();
        System.out.println("Starting sequence sampling...");

        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/deepwalkseq-%s/taxi-crosstime.seq",
                    regionLevel)));
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
        numSamples = 2_000_000; // number of nodes * 1000
        numLayer = 24;
        outputSampleSequence("CA");
    }

}
