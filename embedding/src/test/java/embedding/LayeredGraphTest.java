package embedding;

import junit.framework.TestCase;

/**
 * Test LayeredGraph
 *
 * Created by hxw186 on 1/9/17.
 */
public class LayeredGraphTest extends TestCase {

    public void testAliasTable() {
        LayeredGraph.Vertex org = new LayeredGraph.Vertex("start", 0);
        LayeredGraph.Vertex d1 = new LayeredGraph.Vertex("d1", 1);
        LayeredGraph.Vertex d2 = new LayeredGraph.Vertex("d2", 2);
        LayeredGraph.Vertex d3 = new LayeredGraph.Vertex("d3", 3);
        LayeredGraph.Edge e1 = new LayeredGraph.Edge(org, d1, 2);
        LayeredGraph.Edge e2 = new LayeredGraph.Edge(org, d2, 10);
        LayeredGraph.Edge e3 = new LayeredGraph.Edge(org, d3, 8);

        org.addOutEdge(e1);
        org.addOutEdge(e2);
        org.addOutEdge(e3);

        org.initiateAliasTable();
        /**
         * The alias table looks like this:
         * probTable [0.3, 0.8, 1]
         * aliasTable [1, 2, -1]
         */
        assertEquals(org.aliasTable[0], 1);
        assertEquals(org.aliasTable[1], 2);
        assertEquals(org.aliasTable[2], -1);
        assertEquals(org.probTable[0], 0.3);
        assertEquals(org.probTable[1], 0.8);
        assertEquals(org.probTable[2], 1.0);

        assertEquals(org.outDegree, 20.0);
        assertEquals(org.sampleNextVertex(0.05).id, 1);
        assertEquals(org.sampleNextVertex(0.3).id, 2);
        assertEquals(org.sampleNextVertex(0.4).id, 2);
        assertEquals(org.sampleNextVertex(0.65).id, 3);
        assertEquals(org.sampleNextVertex(0.9).id, 3);
    }

}