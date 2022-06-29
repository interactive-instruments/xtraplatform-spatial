/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.infra.db;

import static de.ii.xtraplatform.base.domain.Constants.TMP_DIR_PROP;

import com.google.common.io.Resources;
import de.ii.xtraplatform.spatialite.domain.SpatialiteLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteJDBCLoader;
import org.sqlite.util.OSInfo;

public class SpatialiteDataSource extends SQLiteDataSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpatialiteDataSource.class);
  private static final Path TMP_DIR =
      Paths.get(System.getProperty(TMP_DIR_PROP)).resolve("spatialite-5.0.1-1");
  private static final String LIB_Z = "libz.so";
  private static final String LIB_GEOS = "libgeos.so";
  private static final String LIB_GEOS_C = "libgeos_c.so";
  private static final String LIB_SQLITE = "libsqlite3.so";
  private static final String MOD_SPATIALITE = "mod_spatialite";
  private static final String MOD_SPATIALITE_SO = "mod_spatialite.so";
  private static final boolean USE_GLOBAL_LIBS;

  // TODO: versioned libs
  // TODO: generalize native lib handling in xtraplatform-native
  static {
    if (OSInfo.getOSName().equalsIgnoreCase("LINUX")
        && OSInfo.getArchName().equals(OSInfo.X86_64)) {
      USE_GLOBAL_LIBS = false;
      try {
        SQLiteJDBCLoader.initialize();
      } catch (Exception e) {
        LOGGER.error("Could not load SQLite: {}", e.getMessage());
      }

      try {
        Files.createDirectories(TMP_DIR);
      } catch (IOException e) {
        throw new IllegalStateException("Could not create directory: " + TMP_DIR);
      }
      copyLibToTmpDir(LIB_Z);
      copyLibToTmpDir(LIB_GEOS);
      copyLibToTmpDir(LIB_GEOS_C);
      copyLibToTmpDir(LIB_SQLITE);
      copyLibToTmpDir(MOD_SPATIALITE_SO);

      loadLib(LIB_Z);
      loadLib(LIB_GEOS);
      loadLib(LIB_GEOS_C);
      // TODO: should be redundant if libspatialite is built against xerial libsqlite
      loadLib(LIB_SQLITE);
    } else {
      LOGGER.warn(
          "GeoPackage/Spatialite is not supported for OS '{}-{}'. It might work if you install libspatialite as a system library.",
          System.getProperty("os.name"),
          System.getProperty("os.arch"));
      USE_GLOBAL_LIBS = true;
    }
  }

  private static void copyLibToTmpDir(String libName) {
    File lib = TMP_DIR.resolve(libName).toFile();
    if (!lib.exists()) {
      try {
        Resources.copy(
            Resources.getResource(SpatialiteLoader.class, "/" + libName),
            new FileOutputStream(lib));
      } catch (IOException e) {
        throw new IllegalStateException("Could not create file: " + lib);
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

    String query =
        USE_GLOBAL_LIBS
            ? String.format("SELECT load_extension('%s');", MOD_SPATIALITE)
            : String.format("SELECT load_extension('%s');", TMP_DIR.resolve(MOD_SPATIALITE));

    try (Statement statement = connection.createStatement()) {
      statement.execute(query);
    }

    return connection;
  }
}
