package embedding;

import junit.framework.TestCase;

/**
 * Created by hxw186 on 1/17/17.
 */
public class TaxiTripIteratorTest extends TestCase {

    public void testTaxiTripIterator() {
        TaxiTripIterator tti = new TaxiTripIterator(2014);

        assertTrue(tti.fileNames.length >= 1);

        while (tti.hasNext());

        assertTrue(tti.numBadTrip < tti.numTotalTrip);
    }

}