package embedding;

import java.io.File;
import java.io.FileInputStream;

import org.nocrala.tools.gis.data.esri.shapefile.ShapeFileReader;
import org.nocrala.tools.gis.data.esri.shapefile.header.ShapeFileHeader;
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolygonMShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolygonShape;

/**
 * Construct the boundary of community areas from shapefile.
 *
 */
public class CommunityAreas
{
    static String dataFolder = "../data";
    static String shapeFilePath = "../data/ChiCA_gps/ChiCaGPS.shp";

    public static void main( String[] args )
    {
        FileInputStream shapeFile = null;
        try {
            shapeFile = new FileInputStream(shapeFilePath );
            ShapeFileReader shr = new ShapeFileReader(shapeFile);

            ShapeFileHeader h = shr.getHeader();
            System.out.println("The shape type of this file is " + h.getShapeType());

            int total = 0;
            AbstractShape shp;
            while ((shp = shr.next()) != null) {
                total ++;
                ShapeType st = shp.getShapeType();
                System.out.println(st);
                switch (st) {
                    case POLYGON:
                        PolygonShape p = (PolygonShape) shp;
                        System.out.format("Polygon with %d parts\n", p.getNumberOfParts());
                        break;
                    case POLYGON_M:
                        PolygonMShape pm = (PolygonMShape) shp;
                        System.out.format("PolygonM with %d parts\n", pm.getNumberOfParts());
                }
            }
            System.out.format("In total, there are %d shapes.\n", total);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println( "Hello World!" );
    }
}
