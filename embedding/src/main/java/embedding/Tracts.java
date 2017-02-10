package embedding;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Construct the boundary of tracts from shapefile.
 */
public class Tracts {
    public static final String shapeFilePath = "../data/Census-Tracts-2010/chicago-tract.shp";
    public static ShapefileDataStore shapefile;
    public static int numTimeSlot = 8;
    public static int Year = 2013;

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

    /**
     * @deprecated mapTripIntoTracts takes a lot of time to run.
     *
     * The {@link Tracts#serializeTracts(int)} function has saved the flow graphs as a file.
     * Use the {@link Tracts#deserialzeTracts(int)} function to retrieve the graph to save time.
     */
    public void mapTripsIntoTracts() {
        mapTripsIntoTracts(tracts.keySet());
    }

    public void mapTripsIntoTracts(Iterator<TaxiTrip> tripsItr) {
        mapTripsIntoTracts(tracts.keySet(), tripsItr);
    }

    public void mapTripsIntoTracts(Set<Integer> focusTracts) {
        List<TaxiTrip> trips = TaxiTrip.parseTaxiFiles();
        mapTripsIntoTracts(focusTracts, trips.iterator());
    }

    public void mapTripsIntoTracts(Set<Integer> focusTracts, Iterator<TaxiTrip> tripsItr) {
        long t1 = System.currentTimeMillis();
        System.out.println("Start mapping trips into tracts...");
        Map<Integer, Tract> focusGroup = new HashMap<>();
        for (int k : focusTracts)
            focusGroup.put(k, tracts.get(k));

        TaxiTrip tt = null;
        while (tripsItr.hasNext()) {
            tt = tripsItr.next();
            Tract s = null, e = null;
            for (Tract t : focusGroup.values()) {
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

    public void serializeTracts(int year) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(String.format("../miscs/tracts-serialize-%d.seq", year)));
            oos.writeInt(numTimeSlot);
            oos.writeObject(tracts);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deserialzeTracts(int year) {
        System.out.format("Deserialize tracts with flow in %d.\n", year);
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(String.format("../miscs/tracts-serialize-%d.seq", year)));
            numTimeSlot = ois.readInt();
            tracts = (HashMap<Integer, Tract>) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void visualizeCasesAsDotFile(int residence, int nightlife, int professional) {
        List<Integer> alltracts = new ArrayList<>(Arrays.asList(new Integer[]{residence, nightlife, professional}));
        for (int hour = 0; hour < 24; hour++) {
            try {
                BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/case-%d.dot", hour)));
                fout.write("digraph { \n");
                fout.write(String.format("%d [color=blue];\n%d [color=red];\n%d [color=green];\n", residence, nightlife, professional));

                for (int src : alltracts) {
                    for (int dst : alltracts) {
                        int w = tracts.get(src).getFlowTo(dst, hour);
                        if (w > 0)
                            fout.write(String.format("%d -> %d [label=\"%d\"];\n", src, dst, w));
                    }
                }
                fout.write("}\n");
                fout.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void visualizeCasesAsDotFile(Set<Integer> residence, Set<Integer> nightlife, Set<Integer> professional) {
        List<Integer> alltracts = Stream.of(residence, nightlife, professional).flatMap(x -> x.stream()).collect(Collectors.toList());

        for (int hour = 0; hour < 24; hour++) {
            try {
                BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/case-%d.dot", hour)));
                fout.write("digraph { \n");
                // plot each node
                for (int r : residence) {
                    fout.write(String.format("%d [color=blue];\n", r));
                }
                for (int nl : nightlife) {
                    fout.write(String.format("%d [color=red];\n", nl));
                }
                for (int pf : professional) {
                    fout.write(String.format("%d [color=green];\n", pf));
                }
                // plot each edge with weight
                for (int src : alltracts) {
                    for (int dst : alltracts) {
                        int w = tracts.get(src).getFlowTo(dst, hour);
                        if (w > 0)
                            fout.write(String.format("%d -> %d [label=\"%d\"];\n", src, dst, w));
                    }
                }
                fout.write("}\n");
                fout.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Generate time series of in/out taxi flow for each regions.
     */
    public void timeSeries_traffic() {
        timeSeries_traffic(tracts.keySet());
    }

    public void timeSeries_traffic(Set<Integer> focusKeys) {
        System.out.format("Generate time series for %d tracts.\n", focusKeys.size());
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter("../miscs/taxi-flow-time-series.txt"));

            for (int k : focusKeys) {
                List<Integer> out = new ArrayList<>();
                List<Integer> in = new ArrayList<>();
                for (int h = 0; h < 24; h++) {
                    int trafficOut = tracts.get(k).taxiFlows.get(h).values().stream().mapToInt(x->x.intValue()).sum();
                    int trafficIn = 0;
                    for (int src : focusKeys) {
                        Map<Integer, Integer> flowMap = tracts.get(src).taxiFlows.get(h);
                        if (flowMap.containsKey(k))
                            trafficIn += flowMap.get(k);
                    }
                    out.add(trafficOut);
                    in.add(trafficIn);
                }
                fout.write(Integer.toString(k));
                for (int fi : in) {
                    fout.write(String.format(",%d", fi));
                }
                fout.write(String.format("\n%d", -k));
                for (int fo : out) {
                    fout.write(String.format(",%d", fo));
                }
                fout.write("\n");
            }
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * =====================================================
     * Build time-slotted taxi flow graphs and output them
     * =====================================================
     */

    /**
     * Output the taxi flow as edge file, namely, each row is an edge, (src, dst, weight). The edge graph is constructed
     * timeslot by timeslot independently, and there will be {@link Tracts#numTimeSlot} edge graphs.
     */
    public void outputEdgeFile(int year) {
        try {
            int timeStep = 24 / numTimeSlot;
            for (int h = 0; h < numTimeSlot; h++) {
                BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/%d/taxi-h%d.od", year, h)));
                for (Tract t : tracts.values()) {
                    for (int dst : t.taxiFlows.get(h).keySet()) {
                        int w = t.getFlowTo(dst, h, h+timeStep-1);
                        if (w > 0)
                            fout.write(String.format("%d %d %d\n", t.id, dst, w));
                    }
                }
                fout.close();
            }

            // one static graph (without temporal dynamics)
            BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/%d/taxi-all.od", year)));
            for (Tract src: tracts.values()) {
                for (Tract dst: tracts.values()) {
                    int w = src.getFlowTo(dst.id, 0, 23);
                    if (w > 0)
                        fout.write(String.format("%d %d %d\n", src.id, dst.id, w));
                }
            }
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Output the hourly taxi flow graph as an adjacency matrix. The graph is exactly the same as {@link Tracts#outputEdgeFile(int year)}.
     */
    public void outputAdjacencyMatrix(int year) {
        try {
            List<Integer> sortedId = new LinkedList<>(tracts.keySet());
            sortedId.sort((a,b) -> a.compareTo(b));
            int timeStep = 24 / numTimeSlot;
            for (int h = 0; h < numTimeSlot; h++) {
                BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/%d/taxi-h%d.matrix", year, h)));
                for (int src : sortedId) {
                    List<String> row = new LinkedList<>();
                    for (int dst : sortedId) {
                        row.add(Integer.toString(tracts.get(src).getFlowTo(dst, h, h+timeStep-1)));
                    }
                    String line = String.join(",", row) + "\n";
                    fout.write(line);
                }
                fout.close();
            }

            // one static graph (without temporal dynamics)
            BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/%d/taxi-all.matrix", year)));
            for (int src: sortedId) {
                List<String> row = new LinkedList<>();
                for (int dst: sortedId) {
                    row.add(Integer.toString(tracts.get(src).getFlowTo(dst, 0, 23)));
                }
                String line = String.join(",", row) + "\n";
                fout.write(line);
            }
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a new edge graph and output it into a file.
     *
     * The crossInterval edge graph is a graph such that within each time interval, there is no edges; cross two consecutive
     * intervals, there are edges and the weight denoting the traffic flow count in previous interval.
     *
     * The basic assumption is that people go to B from A during interval t_i, then they stay there in t_i+1.
     *
     * @deprecated We do not use  word2vec on top of this graph. Instead, we use {@link LayeredGraph}. More
     * specifically, we use {@link CrossTimeGraph} and {@link SpatialGraph}.
     */
    public void outputEdgeGraph_crossInterval() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter("../miscs/taxi-crossInterval.od"));
            for (int h = 0; h < 24; h++) {
                for (Tract t : tracts.values()) {
                    for (Tract dst : tracts.values()) {
                        int w = t.getFlowTo(dst.id, h);
                        if (w > 0)
                            fout.write(String.format("%d-%d %d-%d %d\n", h, t.id, (h+1)%24, dst.id, w));
                    }
                }
            }
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateEdgeFileForEmbedding(int year) {
        Tracts trts = new Tracts();
        trts.deserialzeTracts(year);
        trts.outputEdgeFile(year);  // for graph embedding LINE
        trts.outputAdjacencyMatrix(year);   // for matrix factorization
//        trts.outputEdgeGraph_crossInterval();
    }

    public static void case_by_poi() {
        Tracts tracts = new Tracts();
        Set<Integer> residence = new HashSet<>(Arrays.asList(new Integer[]{32100, 330100, 60800, 80100, 63302})); //
        Set<Integer> nightlife = new HashSet<>(Arrays.asList(new Integer[]{832000, 81700, 62200, 243400})); //
        Set<Integer> professional = new HashSet<>(Arrays.asList(new Integer[]{760801, 81401, 320100, 839100, 81800})); //
        Set<Integer> focusTracts = Stream.of(residence, nightlife, professional).flatMap(x->x.stream()).collect(Collectors.toSet());
        tracts.mapTripsIntoTracts(focusTracts);
        tracts.visualizeCasesAsDotFile(residence, nightlife, professional);
    }

    public static void case_by_timeseries() {
        Tracts tracts = new Tracts();
        Set<Integer> rsd = new HashSet<>(Arrays.asList(new Integer[]{390600, 831100}));
        Set<Integer> prof = new HashSet<>(Arrays.asList(new Integer[]{838200}));
        Set<Integer> nl = new HashSet<>(Arrays.asList(new Integer[]{62500, 61100}));
        Set<Integer> focusTracts = Stream.of(rsd, nl, prof).flatMap(x->x.stream()).collect(Collectors.toSet());
        tracts.mapTripsIntoTracts(focusTracts);
        tracts.visualizeCasesAsDotFile(rsd, nl, prof);
    }

    public static void case_from_bruteForce() {
        int res = 832000, prof = 839100, nl = 831900;
        Tracts tracts = new Tracts();
        Set<Integer> focusTracts = new HashSet<>(Arrays.asList(new Integer[]{res, prof, nl}));
        tracts.mapTripsIntoTracts(focusTracts);
        tracts.visualizeCasesAsDotFile(res, nl, prof);
    }

    /**
     * Enumerate all triplet tracts and check their flow pattern.
     */
    public static void bruteForceSearchCase() {
        long ts = System.currentTimeMillis();
        System.out.println("Start brute force searching possible triplet cases...");

        Tracts tracts = new Tracts();
        tracts.mapTripsIntoTracts();
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter("../miscs/brute-force-pairs.csv"));
            int cnt = 0, posCnt = 0;
            for (Tract t1 : tracts.tracts.values()) {
                for (Tract t2 : tracts.tracts.values()) {
                    if (t2.id == t1.id)
                        continue;
                    for (Tract t3 : tracts.tracts.values()) {
                        if (cnt++ % 1000000 == 0) {
                            System.out.format("%d positive pairs out of %d processed pairs.\n", posCnt, cnt);
                        }
                        if (t3.id == t1.id || t3.id == t2.id)
                            continue;
                        // check pattern by assume t1 is residence, t2 is office, and t3 is night life
                        if (t1.getFlowTo(t2.id, 7, 9) <= 30 ||
                                t3.getFlowTo(t1.id, 17, 23) <= 70 ||
                                t2.getFlowTo(t3.id, 17, 23) <= 70 ||
                                t1.getFlowTo(t2.id, 7, 9) <= 2 * t2.getFlowTo(t1.id, 7, 9) ||   // in the morning, more people go to work than going back from work
                                t1.getFlowTo(t3.id, 17, 22) <= 2 * t1.getFlowTo(t3.id, 7, 12) || // more people go to night club at night than during the day (from home)
                                t2.getFlowTo(t1.id, 17, 22) <= 2 * t1.getFlowTo(t2.id, 17, 22) || // in the afternoon, more people go back home from work than going to work
                                t2.getFlowTo(t3.id, 17, 22) <= 2 * t2.getFlowTo(t3.id, 7, 12) ||  // more people go to night club at night than during the day (from office)
                                t3.getFlowTo(t1.id, 17, 23) <= 2 * t3.getFlowTo(t1.id, 7, 13) ||  // more people go back home from night club at night than during the day
                                t3.getFlowTo(t1.id, 17,23) <= 2 * t3.getFlowTo(t2.id, 17, 23) ||  // at night, more people to back home than to office from night club
                                t3.getFlowTo(t1.id, 17, 23) <= 2 * t1.getFlowTo(t3.id, 17, 23)) {   // at night, more people going back home than going to nightclub
                            continue;
                        } else {
                            posCnt ++;
                            fout.write(String.format("%d\t%d\t%d\n", t1.id, t2.id, t3.id));
                        }
                    }
                }
            }
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long te = System.currentTimeMillis();
        System.out.format("Search complete in %d seconds.\n", (te-ts)/1000);
    }

    public static void main(String[] argv) {
        if (argv.length >= 1) {
            if (argv[0].equals("tracts-ts")) {
                Tracts tracts = new Tracts();
                Set<Integer> foc = new HashSet<>(Arrays.asList(new Integer[]{839100, 81800, 81403, 320100}));
                tracts.deserialzeTracts(2014);
                tracts.timeSeries_traffic(foc);
            } else if (argv[0].equals("case-poi")) {
                case_by_poi();
            } else if (argv[0].equals("case-ts")) {
                case_by_timeseries();
            } else if (argv[0].equals("brute-force-search-case")) {
                bruteForceSearchCase();
            } else if (argv[0].equals("case-brute")) {
                case_from_bruteForce();
            } else if (argv[0].equals("edge-file")) {
                Tracts.Year = 2014;
                generateEdgeFileForEmbedding(Year);
            } else if (argv[0].equals("serialize-tracts")) {
                Tracts tracts = new Tracts();
                Tracts.Year = 2014;
                TaxiTripIterator tti = new TaxiTripIterator(Year);
                tracts.mapTripsIntoTracts(tti);
                tracts.serializeTracts(Year);
            }
        } else {
            System.out.println("Specify task!");
        }
    }
}


class Tract implements Serializable {
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

    public int getFlowTo(int dstId, int hour) {
        return taxiFlows.get(hour).getOrDefault(dstId, 0);
    }
    public int getFlowTo(int dstId, int hourLow, int hourHigh) {
        int cnt = 0;
        for (int h = hourLow; h <= hourHigh; h++)
            cnt += taxiFlows.get(h).getOrDefault(dstId, 0);
        return cnt;
    }

    public Point getCentroid() {
        return this.boundary.getCentroid();
    }

    /**
     * The centroid distance to another tract's centroid.
     * @param o the other tract
     * @return the Euclidean distance of two GPS.
     */
    public double distanceTo(Tract o) {
        Point tc = this.getCentroid();
        Point oc = o.getCentroid();
        return tc.distance(oc);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(id);
        out.writeObject(boundary);
        out.writeObject(taxiFlows);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        id = in.readInt();
        boundary = (MultiPolygon) in.readObject();
        taxiFlows = (List<AbstractMap<Integer, Integer>>) in.readObject();
    }

    private void readOjbectNoData() throws ObjectStreamException {
        id = -1;
        boundary = null;
        taxiFlows = null;
    }
}