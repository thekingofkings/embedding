package embedding;

import java.io.*;
import java.util.Iterator;

/**
 * Created by hxw186 on 1/17/17.
 */
public class TaxiTripIterator implements Iterator<TaxiTrip> {

    int year;
    File taxiDir;
    String[] fileNames;
    int curFileIdx;
    BufferedReader fin;
    int numBadTrip;
    int numTotalTrip;
    TaxiTrip nextTrip;

    public TaxiTripIterator(int year) {
        TaxiTrip.tripFormat = TaxiTrip.TAXITYPE.Type3;
        this.year = year;
        taxiDir = new File(TaxiTrip.tripFilePath);
        fileNames = taxiDir.list((file, s) -> s.matches(String.format("%d\\d+\\.csv", year)));
        curFileIdx = 0;
        setupFileToRead();
        numBadTrip = 0;
        numTotalTrip = 0;
        nextTrip = null;
    }

    public boolean hasNext() {
        if (curFileIdx < fileNames.length && fin != null) {
            try {
                // iterate through current file
                String line;
                while ((line = fin.readLine()) != null) {
                    TaxiTrip t;
                    numTotalTrip++;
                    try {
                        t = new TaxiTrip(line);
                    } catch (Exception e) {
                        numBadTrip++;
                        continue;
                    }
                    nextTrip = t;
                    return true;
                }
                // current file is fully processed, go to next file
                curFileIdx ++;
                setupFileToRead();
                return hasNext();
            } catch (IOException e) {
                e.printStackTrace();
                nextTrip = null;
                return false;
            }
        } else {
            nextTrip = null;
            return false;
        }
    }

    /**
     * Call hasNext() first to generate the next element. Otherwise, always return null.
     * @return
     */
    public TaxiTrip next() {
        return nextTrip;
    }

    private void setupFileToRead() {
        try {
            String fn = taxiDir + File.separator + fileNames[curFileIdx];
            fin = new BufferedReader(new FileReader(fn));
            System.out.format("Start process file %s.\n", fn);
        } catch (ArrayIndexOutOfBoundsException e) {
            fin = null;
        } catch (FileNotFoundException e) {
            System.err.println("TaxiTripIterator - file not found!");
        }
    }
}
