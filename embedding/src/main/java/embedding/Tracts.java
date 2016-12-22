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

    public void visualizeCasesAsDotFile() {
        Set<Integer> residence = new HashSet<>(Arrays.asList(new Integer[]{80100, 63302})); // 32100, 330100, 60800,
        Set<Integer> nightlife = new HashSet<>(Arrays.asList(new Integer[]{832000, 81700})); // , 62200, 243400
        Set<Integer> professional = new HashSet<>(Arrays.asList(new Integer[]{760801, 81401})); // , 320100, 839100, 81800
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
                    Map<Integer, Integer> flowMap = tracts.get(src).taxiFlows.get(hour);  // the flow map of hour
                    for (int dst : alltracts) {
                        if (flowMap.containsKey(dst)) {
                            int w = flowMap.get(dst);
                            if (w > 4)
                                fout.write(String.format("%d -> %d [label=\"%d\"];\n", src, dst, w));
                        }
                    }
                }
                fout.write("}\n");
                fout.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void timeSeries_traffic() {
        timeSeries_traffic(tracts.keySet());
    }

    public void timeSeries_traffic(Set<Integer> focusKeys) {
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


    public static void case_by_poi() {
        Tracts tracts = new Tracts();
        Set<Integer> focusTracts = new HashSet<>(Arrays.asList(new Integer[]{330100, 32100, 63302, 80100, 60800, 81700, 62200, 832000, 243400, 839100, 320100, 81800, 760801, 81401}));
        tracts.mapTripsIntoTracts(focusTracts);
        tracts.visualizeCasesAsDotFile();
        tracts.timeSeries_traffic(focusTracts);
    }

    public static void main(String[] argv) {
        Tracts tracts = new Tracts();
        tracts.mapTripsIntoTracts();
        tracts.timeSeries_traffic();
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
}