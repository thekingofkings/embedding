package embedding;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Connect to chicago_tweets
 * Created by hxw186 on 1/9/17.
 */
public class Tweets {

    public static Map<Integer, Map<Integer, Integer>> tweetsCount;

    public static Connection getMySQLDBConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://lrs-jli02.ist.psu.edu/tweets", "hongjian", "hongjian");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (conn != null) {
            System.out.println("Successfully connect to MySQL.");
            return conn;
        } else {
            return null;
        }
    }

    public static void mapTweetsIntoTracts() {
        long t1 = System.currentTimeMillis();
        Tracts trts = new Tracts();
        Connection c = getMySQLDBConnection();
        tweetsCount = new HashMap<>();
        GeometryFactory pointsBuilder = new GeometryFactory();

        try {
            Statement stmt = c.createStatement();
            stmt.setFetchSize(1000);
            String sql = "select timestamp, lon, lat from chicago_tweets;";
            ResultSet rs = stmt.executeQuery(sql);
            int cnt = 0;
            while (rs.next()) {
                long timestamp = rs.getLong("timestamp");
                double lon = rs.getDouble("lon");
                double lat = rs.getDouble("lat");
                String time = Long.toString(timestamp);
                int hour = Integer.parseInt(time.substring(8, 10));
                if (hour > 23)
                    throw new Exception("Hour is out of range.");
                Coordinate coords = new Coordinate(lon, lat);
                Point p = pointsBuilder.createPoint(coords);
                for (int tid : trts.tracts.keySet()) {
                    Tract t = trts.tracts.get(tid);
                    if (t.boundary.contains(p)) {
                        if (! tweetsCount.containsKey(tid)) {
                            tweetsCount.put(tid, new HashMap<>());
                        }
                        int curCnt = tweetsCount.get(tid).getOrDefault(hour, 0);
                        tweetsCount.get(tid).put(hour, curCnt + 1);
                    }
                }
                cnt ++;
                if (cnt % 50000 == 0)
                    System.out.println(cnt);
            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long t2 = System.currentTimeMillis();
        System.out.format("Map all tweets into tracts finished in %d seconds.\n", (t2-t1)/1000);
    }

    public static void writeOutTweetsCount() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter("../miscs/tweetsCount.txt"));
            for (int tid : tweetsCount.keySet()) {
                fout.write(Integer.toString(tid));
                for (int hour = 0; hour < 24; hour++) {
                    fout.write(String.format(" %d", tweetsCount.get(tid).getOrDefault(hour, 0)));
                }
                fout.write("\n");
            }
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv) {
        mapTweetsIntoTracts();
        writeOutTweetsCount();
    }
}
