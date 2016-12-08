package embedding;

import com.vividsolutions.jts.geom.Point;
import junit.framework.TestCase;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Unit test for TaxiTrip parsing class.
 */
public class TaxiTripTest extends TestCase {

    public void testTaxiFilesExist() throws Exception {
        File taxiDir = new File(TaxiTrip.tripFilePath);
        String[] filesType1 = taxiDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.startsWith("201");
            }
        });
        assertEquals(filesType1.length, 6);

        String[] filesType2 = taxiDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.startsWith("Chicago Verifone ");
            }
        });
        assertEquals(filesType2.length, 2);
    }

    public void testShortDate() {
        String type1Date = "7/27/13 15:48";
        ShortDate d = new ShortDate(type1Date);
        assertEquals(d.day, 27);
        assertEquals(d.hour, 15);
        assertEquals(d.minute, 48);
        assertEquals(d.month, 7);

        ShortDate d2 = new ShortDate(12, 8, 13, 23);
        assertEquals(d2.minute, 23);
        assertEquals(d2.hour, 13);
        assertEquals(d2.day, 8);
        assertEquals(d2.month, 12);
    }

    public void testParseGPS() {
        TaxiTrip.tripFormat = TaxiTrip.TAXITYPE.Type1;
        TaxiTrip t = new TaxiTrip();
        Point p1 = t.parseGPS("\"41.9737,-87.8681\"");
        assertEquals(p1.getX(), 41.9737);
        assertEquals(p1.getY(), -87.8681);
    }

    public void testTaxiTrip() {
        String s = "7/27/13 20:00\tCredit Card\t19\t3.25\t0\t0\t0\t7/27/13 15:00\t7/27/13 15:00\t\"41.887,-87.6957\"\t\"41.887,-87.6957\"\t7/27/13 20:00\t7/27/13 20:00";
        TaxiTrip t = new TaxiTrip(s);
        assertEquals(t.duration, 19);
        assertEquals(t.endLoc.getX(), 41.887);
        assertEquals(t.startLoc.getY(), -87.6957);
        assertEquals(t.startDate.hour, 15);
        assertEquals(t.endDate.day, 27);
    }

}