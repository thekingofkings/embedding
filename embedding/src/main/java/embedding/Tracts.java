package embedding;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
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

    public void mapTripsIntoTracts() {
        mapTripsIntoTracts(tracts.keySet());
    }

    public void mapTripsIntoTracts(Set<Integer> focusKeys) {
        long t1 = System.currentTimeMillis();
        System.out.println("Start mapping trips into tracts...");
        Map<Integer, Tract> focusGroup = new HashMap<>();
        for (int k : focusKeys)
            focusGroup.put(k, tracts.get(k));


        List<TaxiTrip> trips = TaxiTrip.parseTaxiFiles();
        for (TaxiTrip tt : trips) {
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
     * Build hourly taxi flow graphs and output them
     * =====================================================
     */

    /**
     * Output the taxi flow as edge file, namely, each row is an edge, (src, dst, weight). The edge graph is constructed
     * hour by hour independently, and there will be 24 edge graphs.
     */
    public void outputEdgeFile() {
        try {
            for (int h = 0; h < 24; h++) {
                BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/taxi-h%d.od", h)));
                for (Tract t : tracts.values()) {
                    for (int dst : t.taxiFlows.get(h).keySet()) {
                        int w = t.getFlowTo(dst, h);
                        if (w > 0)
                            fout.write(String.format("%d %d %d\n", t.id, dst, w));
                    }
                }
                fout.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Output the hourly taxi flow graph as an adjacency matrix. The graph is exactly the same as {@link Tracts#outputEdgeFile()}.
     */
    public void outputAdjacencyMatrix() {
        try {
            for (int h = 0; h < 24; h++) {
                BufferedWriter fout = new BufferedWriter(new FileWriter(String.format("../miscs/taxi-h%d.matrix", h)));
                List<Integer> sortedId = new LinkedList<>(tracts.keySet());
                sortedId.sort((a,b) -> a.compareTo(b));
                for (int src : sortedId) {
                    List<String> row = new LinkedList<>();
                    for (int dst : sortedId) {
                        row.add(Integer.toString(tracts.get(src).getFlowTo(dst, h)));
                    }
                    String line = String.join(",", row) + "\n";
                    fout.write(line);
                }
                fout.close();
            }
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

    public static void generateEdgeFileForEmbedding() {
        Tracts trts = new Tracts();
        trts.mapTripsIntoTracts();
        trts.outputEdgeFile();  // for graph embedding LINE
        trts.outputAdjacencyMatrix();   // for matrix factorization
        trts.outputEdgeGraph_crossInterval();
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
                tracts.mapTripsIntoTracts(foc);
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
                generateEdgeFileForEmbedding();
            }
        } else {
            System.out.println("Specify task!");
        }
    }
}


class Tract {
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
}