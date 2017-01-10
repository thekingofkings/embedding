package embedding;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Created by hxw186 on 1/10/17.
 */
public class SpatialGraphTest extends TestCase {
    public void testKeepNearestKVertices() throws Exception {
        SpatialGraph g = SpatialGraph.constructGraph();
        g.keepNearestKVertices(10);
        LayeredGraph.Vertex v = g.allVertices.get("63302");
        v.edgesOut.sort((v1, v2) -> - Double.compare(v1.weight, v2.weight));
        double[] vweights = v.edgesOut.stream().mapToDouble(e -> e.weight).toArray();

        assertEquals(vweights.length, 10);
        assertTrue(vweights[0] > vweights[2]);
        assertTrue(vweights[2] > vweights[4]);
        assertTrue(vweights[4] > vweights[9]);
    }

}