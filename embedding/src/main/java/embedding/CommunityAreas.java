package embedding;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Construct the boundary of community areas from shapefile.
 *
 */
public class CommunityAreas
{
    static String dataFolder = "../data";

    /**
     * There are 10 fields for each SimpleFeature samples in this shapefile.
     * We will only use the following three:
     *      1. MultiPolygon (boundary: MultiPolygon)
     *      2. AREA_NUMBE (id: int)
     *      3. COMMUNITY (name: string)
     */
    public static final String shapeFilePath = "../data/ChiCA_gps/ChiCaGPS.shp";

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
                    int curCount = s.taxiFlows.get(e.id-1);
                    s.taxiFlows.set(e.id-1, curCount+1);
                    break;
                }
            }
        }

        long t2 = System.currentTimeMillis();
        System.out.format("Map trips into communities finished in %s seconds.\n", (t2-t1)/1000);
        saveTaxiFlowMatrix();
    }

    private void saveTaxiFlowMatrix() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter("taxiFlow.csv"));
            for (int i = 1; i <= communities.size(); i++) {
                List<Integer> flows = communities.get(i).taxiFlows;
                List<String> flowStr = flows.stream().map(x -> x.toString()).collect(Collectors.toList());
                String line = flowStr.stream().collect(Collectors.joining(","));
                fout.write(line + "\n");
            }
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate edge graph for LINE embedding learning
     */
    public void generateLINEtaxiOD() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter("taxiOD.csv"));
            for (int i = 1; i <= communities.size(); i++) {
                for (int j = 0; j < 77; j++) {
                    fout.write(String.format("%d %d %d\n", i, j+1, communities.get(i).taxiFlows.get(j)));
                }
            }
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main( String[] args )
    {
        CommunityAreas CAs = new CommunityAreas();
        CAs.mapTripsIntoCommunities();
        CAs.generateLINEtaxiOD();
    }
}


class CommunityArea {
    int id;     // index start at 1
    String name;
    MultiPolygon boundary;
    List<Integer> taxiFlows;

    public CommunityArea(int id, String name, MultiPolygon boundary, int count) {
        this.id = id;
        this.name = name;
        this.boundary = boundary;
        this.taxiFlows = new ArrayList<>(Collections.nCopies(count, 0));
    }

    public CommunityArea(int id, String name, MultiPolygon boundary) {
        this(id, name, boundary, 77);
    }

    public Geometry getBoundary(){
        return this.boundary;
    }
}
