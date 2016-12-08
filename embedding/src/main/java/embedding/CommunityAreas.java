package embedding;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;


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
    static String shapeFilePath = "../data/ChiCA_gps/ChiCaGPS.shp";

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main( String[] args )
    {
        CommunityAreas CAs = new CommunityAreas();
    }
}


class CommunityArea {
    int id;
    String name;
    MultiPolygon boundary;

    public CommunityArea(int id, String name, MultiPolygon boundary) {
        this.id = id;
        this.name = name;
        this.boundary = boundary;
    }

    public Geometry getBoundary(){
        return this.boundary;
    }
}
