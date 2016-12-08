package embedding;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
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
 * Unit test for simple CommunityAreas.
 */
public class CommunityAreasTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public CommunityAreasTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( CommunityAreasTest.class );
    }

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

            assertEquals(total, 77);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testCommunityAreas() {
        CommunityAreas CAs = new CommunityAreas();
        Set<Integer> keys = CAs.communities.keySet();

        for (int i = 1; i <= 77; i++) {
            assert keys.contains(i);
            Point centroid = CAs.communities.get(i).getBoundary().getCentroid();
            assertTrue(centroid.getX() != 0);
            assertTrue(centroid.getY() != 0);
        }
    }
}
