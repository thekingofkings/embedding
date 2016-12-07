package embedding;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.nocrala.tools.gis.data.esri.shapefile.ShapeFileReader;
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolygonShape;

import java.io.File;
import java.io.FileInputStream;

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
            FileInputStream fin = new FileInputStream(CommunityAreas.shapeFilePath);
            ShapeFileReader r = new ShapeFileReader(fin);

            int total = 0;
            boolean allPolygon = true;
            AbstractShape shp;
            while ((shp = r.next()) != null) {
                total ++;
                allPolygon = allPolygon && (shp.getShapeType() == ShapeType.POLYGON);
            }
            assertEquals(total, 77);
            assertTrue(allPolygon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
