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
                return s.matches("201[34]-.*");
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

        ShortDate d3 = new ShortDate("5/6/2013", "12:34:00 PM");
        assertEquals(d3.minute, 34);
        assertEquals(d3.hour, 12);
        assertEquals(d3.day, 6);
        assertEquals(d3.month, 5);
    }

    public void testParseGPST1() {
        TaxiTrip.tripFormat = TaxiTrip.TAXITYPE.Type1;
        TaxiTrip t = new TaxiTrip();
        Point p1 = t.parseGPSType1("\"41.9737,-87.8681\"");
        assertEquals(p1.getX(), -87.8681);
        assertEquals(p1.getY(), 41.9737);
    }

    public void testTaxiTripT1() {
        TaxiTrip.tripFormat = TaxiTrip.TAXITYPE.Type1;
        String s = "7/27/13 20:00\tCredit Card\t19\t3.25\t0\t0\t0\t7/27/13 15:00\t7/27/13 15:00\t\"41.887,-87.6957\"\t\"41.887,-87.6957\"\t7/27/13 20:00\t7/27/13 20:00";
        try {
            TaxiTrip t = new TaxiTrip(s);
            assertEquals(t.duration, 19);
            assertEquals(t.endLoc.getX(), -87.6957);
            assertEquals(t.startLoc.getY(), 41.887);
            assertEquals(t.startDate.hour, 15);
            assertEquals(t.endDate.day, 27);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testTaxiTripT2() {
        TaxiTrip.tripFormat = TaxiTrip.TAXITYPE.Type2;
        String s1 = "6/10/2013 0:00\t12:07:00 AM\t6/10/2013 0:11\t5.85\t0\t0\t0\t5.85\t0\t-87.6751\t41.9088\t-87.6524\t41.9089\t1\tCASH\t240\t5.85";
        String s2 = "5/12/2013\t7:17:00 PM\t5/12/2013 19:22\t6.85\t0\t0\t0\t6.85\t0\t-87.666\t41.9326\t-87.6924\t41.9249\t1\tCASH\t300\t6.85";
        try {
            TaxiTrip t = new TaxiTrip(s1);
            assertEquals(t.duration, 240);
            assertEquals(t.endDate.hour, 0);
            assertEquals(t.endDate.minute, 11);
            assertEquals(t.endLoc.getX(), -87.6524);
            assertEquals(t.endLoc.getY(), 41.9089);
            assertEquals(t.startLoc.getX(), -87.6751);
            assertEquals(t.startLoc.getY(), 41.9088);
            assertEquals(t.startDate.day, 10);
            assertEquals(t.startDate.minute, 7);
            assertEquals(t.startDate.hour, 0);

            TaxiTrip t2 = new TaxiTrip(s2);
            assertEquals(t2.startDate.hour, 19);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}