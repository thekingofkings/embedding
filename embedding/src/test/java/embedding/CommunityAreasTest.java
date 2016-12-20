package embedding;

import com.vividsolutions.jts.geom.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.geotools.data.FeatureReader;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.Geometry;

import java.io.File;
import java.util.Set;

/**
 * Unit test for class CommunityAreas.
 */
public class CommunityAreasTest
    extends TestCase
{
    /**
     * Rigourous Test :-)
     */
    public void testShapeFileExists() {
        File f = new File(CommunityAreas.shapeFilePath);
        assertTrue(f.exists());
    }

    public void testAllShapesArePolygon() {
        try {
            File fin = new File(CommunityAreas.shapeFilePath);
            ShapefileDataStore r = new ShapefileDataStore(fin.toURI().toURL());
            SimpleFeatureType sft = r.getSchema();

            assertEquals(sft.getTypes().size(), 10);
            assertEquals(sft.getType(0).getName().getLocalPart(), "MultiPolygon");
            assertEquals(sft.getType(5).getName().getLocalPart(), "AREA_NUMBE");
            assertEquals(sft.getType(6).getName().getLocalPart(), "COMMUNITY");

            FeatureReader<SimpleFeatureType, SimpleFeature> fr = r.getFeatureReader();

            SimpleFeature shp;

            int total = 0;
            while (fr.hasNext()) {
                total ++;
                shp = fr.next();
                MultiPolygon g = (MultiPolygon) shp.getDefaultGeometry();
            }

            fr.close();
            r.dispose();

            assertEquals(total, 77);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testBuildingCommunityAreas() {
        CommunityAreas CAs = new CommunityAreas();
        Set<Integer> keys = CAs.communities.keySet();

        for (int i = 1; i <= 77; i++) {
            assert keys.contains(i);
            Point centroid = CAs.communities.get(i).getBoundary().getCentroid();
            assertTrue(centroid.getX() != 0);
            assertTrue(centroid.getY() != 0);
        }
    }

    public void testGeometryOperation() {
        GeometryFactory gf = new GeometryFactory();
        Coordinate[] polyCoords = {
                new Coordinate(0, 0),
                new Coordinate(0, 10),
                new Coordinate(12, 0),
                new Coordinate(0,0)
        };
        Polygon poly = gf.createPolygon(polyCoords);
        Point p = gf.createPoint(new Coordinate(1, 3));
        assertTrue(poly.contains(p));
        assertTrue(p.within(poly));
    }
}
