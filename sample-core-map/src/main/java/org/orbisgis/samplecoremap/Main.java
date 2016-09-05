package org.orbisgis.samplecoremap;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;
import org.h2.util.OsgiDataSourceFactory;
import org.h2gis.h2spatialext.CreateSpatialExtension;
import org.h2gis.utilities.JDBCUrlParser;
import org.h2gis.utilities.SFSUtilities;
import org.orbisgis.commons.progress.NullProgressMonitor;
import org.orbisgis.core_export.MapImageWriter;
import org.orbisgis.corejdbc.DataManager;
import org.orbisgis.corejdbc.internal.DataManagerImpl;
import org.orbisgis.coremap.layerModel.MapContext;
import org.orbisgis.coremap.layerModel.OwsMapContext;
import org.osgi.service.jdbc.DataSourceFactory;

/**
 * Do the following things:
 * - Create a H2 database
 * - Open OWS Context
 * - Render then save it in png file.
 * @author Nicolas Fortin
 */
public class Main {
    public static void main (String[] args) {
        try {
            // Use H2 DataSourceFactory
            DataSourceFactory dataSourceFactory = new OsgiDataSourceFactory(org.h2.Driver.load());
            // Set properties for in-memory connection
            Properties properties = JDBCUrlParser.parse("jdbc:h2:/"+new File("sample_db").getAbsolutePath());
            // Create instance of data manager.
            DataManager dataManager = new DataManagerImpl(SFSUtilities.wrapSpatialDataSource(
                    dataSourceFactory.createDataSource(properties)));
            // Init spatial functions
            try(Connection connection = dataManager.getDataSource().getConnection();
                    Statement st = connection.createStatement()) {
                // Import spatial functions, domains and drivers
                // If you are using a file database, you have to do only that once.
                st.execute("DROP TABLE IF EXISTS landcover2000");
                CreateSpatialExtension.initSpatialExtension(connection);
                // Read OWS context file
                MapContext mc = new OwsMapContext(dataManager);
                String owsFile = "landcover2000.ows";
                // Set where to fetch resources
                mc.setLocation(Main.class.getResource("landcover2000.ows").toURI());
                mc.read(Main.class.getResourceAsStream(owsFile));
                // Read the file
                // Open it (it will link the database with the shape file)
                mc.open(new NullProgressMonitor());
                // Render it in buffered image
                MapImageWriter mapImageWriter = new MapImageWriter(mc.getLayerModel());
                FileOutputStream out = new FileOutputStream(new File("landcover2000.png").getAbsoluteFile());
                mapImageWriter.setFormat(MapImageWriter.Format.PNG);
                mapImageWriter.write(out, new NullProgressMonitor());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
