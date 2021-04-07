package de.ii.xtraplatform.feature.provider.sql.infra.db;

import static de.ii.xtraplatform.runtime.domain.Constants.TMP_DIR_PROP;

import com.google.common.io.Resources;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteDataSource;
import org.sqlite.util.OSInfo;

public class SpatialiteDataSource extends SQLiteDataSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpatialiteDataSource.class);
  private static final Path TMP_DIR = Paths.get(System.getProperty(TMP_DIR_PROP));
  private static final String LIB_GEOS = "libgeos.so";
  private static final String LIB_GEOS_C = "libgeos_c.so";
  private static final String MOD_SPATIALITE = "mod_spatialite";
  private static final String MOD_SPATIALITE_SO = "mod_spatialite.so";

  //TODO: versioned libs
  //TODO: generalize native lib handling in xtraplatform-native
  static {
    if (OSInfo.getOSName().equalsIgnoreCase("LINUX") && OSInfo.getArchName().equals(OSInfo.X86_64)) {
      OSInfo.getNativeLibFolderPathForCurrentOS();

      copyLibToTmpDir(LIB_GEOS);
      copyLibToTmpDir(LIB_GEOS_C);
      copyLibToTmpDir(MOD_SPATIALITE_SO);

      loadLib(LIB_GEOS);
      loadLib(LIB_GEOS_C);
    } else {
      LOGGER.warn("GeoPackage/Spatialite is not supported for OS '{}-{}'. It might work if you install libspatialite as a system library.", System.getProperty("os.name"), System.getProperty("os.arch"));
    }
  }

  private static void copyLibToTmpDir(String libName) {
    File lib = TMP_DIR.resolve(libName).toFile();
    if (!lib.exists()) {
      try {
        Resources.copy(Resources.getResource(libName), new FileOutputStream(lib));
      } catch (IOException e) {
        throw new IllegalStateException("Could not create file: " + lib.toString());
      }
    }
  }

  private static void loadLib(String libName) {
    Path lib = TMP_DIR.resolve(libName);
    try {
      System.load(lib.toString());
    } catch (Throwable e) {
      throw new IllegalStateException("Could not load library: " + lib.toString());
    }
  }

  @Override
  public SQLiteConnection getConnection(String username, String password) throws SQLException {
    SQLiteConnection connection = super.getConnection(username, password);

    String query = String.format("SELECT load_extension('%s');", TMP_DIR.resolve(MOD_SPATIALITE));

    try (Statement statement = connection.createStatement()) {
      statement.execute(query);
    }

    return connection;
  }
}
