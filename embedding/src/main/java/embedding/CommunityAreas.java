package embedding;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import java.io.*;
import java.util.*;

/**
 * Construct the boundary of community areas from shapefile.
 *
 */
public class CommunityAreas
{
    /**
     * There are 10 fields for each SimpleFeature samples in this shapefile.
     * We will only use the following three:
     *      1. MultiPolygon (boundary: MultiPolygon)
     *      2. AREA_NUMBE (id: int)
     *      3. COMMUNITY (name: string)
     */
    static final String shapeFilePath = "../data/ChiCA_gps/ChiCaGPS.shp";

    public AbstractMap<Integer, CommunityArea> communities;

    public CommunityAreas() {
        communities = new HashMap<>();
        try {
            File f = new File(shapeFilePath);
            ShapefileDataStore shapefile = new ShapefileDataStore(f.toURI().toURL());

            SimpleFeatureIterator features = shapefile.getFeatureSource().getFeatures().features();
            SimpleFeature shp;
            while (features.hasNext()) {
                shp = features.next();
                int id = Integer.parseInt((String) shp.getAttribute("AREA_NUMBE"));
                String name = (String) shp.getAttribute("COMMUNITY");
                MultiPolygon boundary = (MultiPolygon) shp.getDefaultGeometry();
                CommunityArea ca = new CommunityArea(id, name, boundary);
                communities.put(id, ca);
            }
            features.close();
            shapefile.dispose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void mapTripsIntoCommunities() {
        long t1 = System.currentTimeMillis();
        System.out.println("Start mapping trips into communities.");

        List<TaxiTrip> trips = TaxiTrip.parseTaxiFiles();
        for (TaxiTrip t : trips) {
            CommunityArea s = null, e = null;
            for (CommunityArea ca : communities.values()) {
                if (ca.boundary.contains(t.startLoc))
                    s = ca;
                if (ca.boundary.contains(t.endLoc))
                    e = ca;
                if (s != null && e != null) {
                    int hour = t.startDate.hour;
                    int curCount = s.getFlowTo(e.id, hour);
                    s.taxiFlows.get(hour).put(e.id, curCount+1);
                    break;
                }
            }
        }

        long t2 = System.currentTimeMillis();
        System.out.format("Map trips into communities finished in %s seconds.\n", (t2-t1)/1000);
    }

    private void outputStaticFlowGraph() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter("../miscs/taxi-CA-static.matrix"));
            BufferedWriter fout2 = new BufferedWriter(new FileWriter("../miscs/taxi-CA-static.od"));
            for (int i = 1; i <= communities.size(); i++) {
                List<String> row = new LinkedList<>();
                for (int j = 1; j <= communities.size(); j++) {
                    int w = communities.get(i).getFlowTo(j, 0, 23);
                    row.add(Integer.toString(w));
                    if (w > 0)
                        fout2.write(String.format("%d %d %d\n", i, j, w));
                }
                String line = String.join(",", row);
                fout.write(line + "\n");
            }
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void outputAdjacencyMatrix() {
        try {
            for (int hour = 0; hour < 24; hour ++) {
                BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/taxi-CA-h%d.matrix", hour)));
                for (int i = 1; i <= communities.size(); i++) {
                    List<String> row = new LinkedList<>();
                    for (int j = 1; j <= communities.size(); j++) {
                        row.add(Integer.toString(communities.get(i).getFlowTo(j, hour)));
                    }
                    String line = String.join(" ", row);
                    fout.write(line + "\n");
                }
                fout.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate edge graph for LINE embedding learning
     */
    public void outputEdgeGraph_LINE() {
        try {
            for (int hour = 0; hour < 24; hour++) {
                BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/taxi-CA-h%d.od", hour)));
                for (int i = 1; i <= communities.size(); i++) {
                    for (int j = 1; j < communities.size(); j++) {
                        fout.write(String.format("%d %d %d\n", i, j, communities.get(i).getFlowTo(j, hour)));
                    }
                }
                fout.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main( String[] args )
    {
        CommunityAreas CAs = new CommunityAreas();
        CAs.mapTripsIntoCommunities();
        CAs.outputStaticFlowGraph();
//        CAs.outputEdgeGraph_LINE();
//        CAs.outputAdjacencyMatrix();
    }
}


class CommunityArea {
    int id;     // index start at 1
    String name;
    MultiPolygon boundary;
    /**
     * taxiFlows is a nested map.
     * The index of the first layer is the hour of day (0-23)
     * The key for the second layer is destination CA ID.
     * The value is the flow from current CA to destination CA.
     */
    List<AbstractMap<Integer, Integer>> taxiFlows;

    public CommunityArea(int id, String name, MultiPolygon boundary) {
        this.id = id;
        this.name = name;
        this.boundary = boundary;
        this.taxiFlows = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            taxiFlows.add(new HashMap<>());
        }
    }

    public Geometry getBoundary(){
        return this.boundary;
    }

    public int getFlowTo(int dstId, int hour) {
        return taxiFlows.get(hour).getOrDefault(dstId, 0);
    }

    public int getFlowTo(int dstId, int hourLow, int hourHigh) {
        int cnt = 0;
        for (int h = hourLow; h <= hourHigh; h++)
            cnt += taxiFlows.get(h).getOrDefault(dstId, 0);
        return cnt;
    }

    public Point getCentroid() {
        return this.boundary.getCentroid();
    }

    /**
     * The centroid distance to another CA's centroid.
     * @param o the other CA
     * @return the Euclidean distance of two GPS.
     */
    public double distanceTo(CommunityArea o) {
        Point tc = this.getCentroid();
        Point oc = o.getCentroid();
        return tc.distance(oc);
    }
}
