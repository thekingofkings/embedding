package embedding;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import junit.framework.TestCase;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Unit test for Tracts class.
 */
public class TractsTest extends TestCase {

    public void testShapeFileExists() {
        File f = new File(Tracts.shapeFilePath);
        assertTrue(f.exists());
    }

    public void testAllShapesArePolygon() {
        try {
            SimpleFeatureIterator features = Tracts.getShapeFileFeatures();
            SimpleFeature shp = features.next();

            int fieldSize = shp.getType().getTypes().size();
            assertEquals(fieldSize, 10);
            assertEquals(shp.getType().getType(3).getName().getLocalPart(), "tractce10");
            assertEquals(shp.getType().getType(0).getName().getLocalPart(), "MultiPolygon");
            for (int i = 0; i < fieldSize; i++){
                System.out.println(shp.getType().getType(i).getName().getLocalPart());
            }

            int cnt = 1;
            while (features.hasNext()) {
                shp = features.next();
                MultiPolygon g = (MultiPolygon) shp.getDefaultGeometry();
                cnt ++;
            }
            assertEquals(cnt, 801);
            features.close();
            Tracts.shapefile.dispose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testSortedIDs() {
        Tracts trts = new Tracts();
        List<Integer> sortedIds = new LinkedList<>(trts.tracts.keySet());
        sortedIds.sort((a, b) -> a.compareTo(b));
        assertEquals(sortedIds.size(), 801);
        assertEquals((int) sortedIds.get(0), 10100);
        assertEquals((int) sortedIds.get(800), 980100);
        assertTrue(sortedIds.get(0) < sortedIds.get(800));
    }

    public void testTractCentroidDistance() {
        GeometryFactory gf = new GeometryFactory();

        Coordinate[] coords1 = new Coordinate[]{
                new Coordinate(0, 0), new Coordinate(2, 0), new Coordinate(2,2),
                new Coordinate(0, 2), new Coordinate(0, 0)
        };
        MultiPolygon mp1 = gf.createMultiPolygon(new Polygon[] {gf.createPolygon(coords1)});
        Tract t1 = new Tract(1, mp1);
        assertEquals(t1.getCentroid().getX(), 1.0);
        assertEquals(t1.getCentroid().getY(), 1.0);

        Coordinate[] coords2 = new Coordinate[]{
                new Coordinate(0, 10), new Coordinate(2, 10), new Coordinate(2,12),
                new Coordinate(0, 12), new Coordinate(0, 10)
        };
        MultiPolygon mp2 = gf.createMultiPolygon(new Polygon[] {gf.createPolygon(coords2)});
        Tract t2 = new Tract(2, mp2);
        assertEquals(t2.getCentroid().getX(), 1.0);
        assertEquals(t2.getCentroid().getY(), 11.0);

        assertEquals(t1.distanceTo(t2), 10.0);
    }

    public void testSerialize() {
        Tracts trts = new Tracts();

        List<TaxiTrip> trips = new ArrayList<>();
        TaxiTrip.tripFormat = TaxiTrip.TAXITYPE.Type1;
        TaxiTrip.parseTaxiFilesHelper(new File(TaxiTrip.tripFilePath), (file, s) -> s.matches("201[34]-.*"), trips);

        trts.mapTripsIntoTracts(trips.iterator());
        trts.serializeTracts(2014);
        AbstractMap<Integer, Tract> org_trts = trts.tracts;
        int org_nts = Tracts.numTimeSlot;

        Tracts trts2 = new Tracts();
        trts2.deserialzeTracts(2014);
        AbstractMap<Integer, Tract> new_trts = trts2.tracts;

        assertEquals(Tracts.numTimeSlot, org_nts);
//        assertEquals(org_trts.get());

    }

}