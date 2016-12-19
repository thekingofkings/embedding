package embedding;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;

/**
 * Construct the boundary of tracts from shapefile.
 */
public class Tracts {
    public static final String shapeFilePath = "../data/Census-Tracts-2010/chicago-tract.shp";
    public static ShapefileDataStore shapefile;

    public AbstractMap<Integer, Tract> tracts;

    public Tracts() {
        tracts = new HashMap<>();
        try {
            SimpleFeatureIterator features = getShapeFileFeatures();
            SimpleFeature shp;
            while (features.hasNext()) {
                shp = features.next();
                int id = Integer.parseInt((String) shp.getAttribute("tractce10"));
                MultiPolygon boundary = (MultiPolygon) shp.getDefaultGeometry();
                Tract t = new Tract(id, boundary);
                tracts.put(id, t);
            }
            features.close();
            shapefile.dispose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SimpleFeatureIterator getShapeFileFeatures() throws IOException {
        File f = new File(shapeFilePath);
        shapefile = new ShapefileDataStore(f.toURI().toURL());
        SimpleFeatureIterator features = shapefile.getFeatureSource().getFeatures().features();
        return features;
    }

    public void mapTripsIntoTracts() {
        long t1 = System.currentTimeMillis();
        System.out.println("Start mapping trips into tracts...");

        List<TaxiTrip> trips = TaxiTrip.parseTaxiFiles();
        for (TaxiTrip tt : trips) {
            Tract s = null, e = null;
            for (Tract t : tracts.values()) {
                if (t.boundary.contains(tt.startLoc))
                    s = t;
                if (t.boundary.contains(tt.endLoc))
                    e = t;
                if (s != null && e != null) {
                    int hour = tt.startDate.hour;
                    if (s.taxiFlows.get(hour).containsKey(e.id)) {
                        int curCount = s.taxiFlows.get(hour).get(e.id);
                        s.taxiFlows.get(hour).put(e.id, curCount+1);
                    } else {
                        s.taxiFlows.get(hour).put(e.id, 1);
                    }
                    break;
                }
            }
        }

        long t2 = System.currentTimeMillis();
        System.out.format("Map trips into tracts finished in %s seconds.\n", (t2-t1)/1000);
    }


    public static void main(String[] argv) {
        Tracts tracts = new Tracts();
        tracts.mapTripsIntoTracts();
    }
}


class Tract {
    int id;
    MultiPolygon boundary;

    /*
    taxiFlows is a nested map.
    The index of the first layer is the hour of day (0 - 23).
    The key of the second layer is the destination tract ID.
    The value is the taxi trip count.
     */
    List<AbstractMap<Integer, Integer>> taxiFlows;

    public Tract(int id, MultiPolygon boundary) {
        this.id = id;
        this.boundary = boundary;
        taxiFlows = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            taxiFlows.add(new HashMap<>());
        }
    }

    public Geometry getBoundary() {
        return this.boundary;
    }
}