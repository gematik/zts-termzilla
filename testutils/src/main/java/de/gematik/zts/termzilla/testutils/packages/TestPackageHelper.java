/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 */
package de.gematik.zts.termzilla.testutils.packages;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestPackageHelper {

  private TestPackageHelper() {
    // Private constructor to prevent instantiation
  }

  /**
   * @param packageFile Pfad- und Dateiname de
   * @param copyToLocation
   * @return
   */
  public static Path copyTestPackage(String packageFile, Path copyToLocation) {

    Path packagePath = Paths.get(packageFile);

    // Zielpfad im Dateisystem
    Path targetPath = Paths.get(copyToLocation.toString(), packagePath.getFileName().toString());

    // Datei aus dem Ressourcenverzeichnis laden
    try (InputStream resourceStream =
        TestPackageHelper.class.getClassLoader().getResourceAsStream(packageFile)) {

      if (resourceStream == null) {
        throw new FileNotFoundException("Ressource nicht gefunden: " + packageFile);
      }

      // Datei zum Zielpfad kopieren
      Files.copy(resourceStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
      return targetPath;
    } catch (IOException e) {
      throw new IllegalStateException("Fehler beim Kopieren der Testdatei: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes a package directory and all its contents recursively. If a file or directory no longer
   * exists, the deletion is skipped silently.
   *
   * @param packageDir the directory path to delete
   * @throws IOException if an I/O error occurs during deletion
   */
  public static void deletePackageDirectory(Path packageDir) throws IOException {
    if (!Files.exists(packageDir)) {
      log.debug("Directory does not exist, skipping deletion: {}", packageDir);
      return;
    }

    try (Stream<Path> pathStream = Files.walk(packageDir)) {
      pathStream
          .sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  log.info("Deleting file/folder: {}", path);
                  Files.delete(path);
                } catch (NoSuchFileException _) {
                  log.debug("File/folder already deleted: {}", path);
                } catch (IOException e) {
                  log.error("Failed to delete: {}", path, e);
                  throw new UncheckedIOException("Failed to delete: " + path, e);
                }
              });
    }
  }
}
