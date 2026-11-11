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
package de.gematik.zts.termzilla.terminologies.fhirterms.memory.loader;

import de.gematik.zts.termzilla.terminologies.fhirterms.fhirpackage.model.packagemanifest.PackageManifest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.MetadataResource;

@Slf4j
public class LoaderHelper {

  // TODO: Der Namespace sollte sich eigentlich aus der TX-Version, TX-Konfiguration und der
  //  FHIR-Modul-Konfiguration ergeben. Das müssen wir in der nächsten Iteration nachziehen
  private static final UUID NAMESPACE = UUID.fromString("8f008b5e-a132-48c0-ac2f-b112c74ca898");

  private LoaderHelper() {}

  /**
   * Generates a deterministic UUID v5 identifier for a FHIR resource.
   *
   * <p>The identifier is derived from the resource URL, version, package coordinates, and content
   * hash. If no URL is available, a random UUID is returned as fallback.
   *
   * @param url canonical URL of the resource
   * @param version version of the resource
   * @param packageName name of the FHIR package containing the resource
   * @param packageVersion version of the FHIR package containing the resource
   * @param resourceHash SHA-256 hash of the resource content
   * @return a name-based UUID v5 string, or a random UUID if {@code url} is {@code null}
   */
  private static String generateId(
      String url, String version, String packageName, String packageVersion, String resourceHash) {

    if (url != null) {
      // Compose a deterministic name from resource and package identifiers
      StringBuilder composedId = new StringBuilder(url);
      if (version != null) {
        composedId.append('-').append(version);
      }
      if (packageName != null) {
        composedId.append('-').append(packageName);
      }
      if (packageVersion != null) {
        composedId.append('-').append(packageVersion);
      }
      if (resourceHash != null) {
        composedId.append('-').append(resourceHash);
      }

      return uuidV5(NAMESPACE, composedId.toString().getBytes(StandardCharsets.UTF_8)).toString();
    } else {
      // Für den Fall, dass wir keine URL haben, generieren wir eine zufällige UUID - Das dürfte
      // jedoch eigentlich nicht passieren
      // TODO: Vielleicht sollten wir hier stattdessen eine Ausnahme werfen, da eine fehlende URL
      //  eigentlich ein Fehler in der Datenquelle darstellt?
      return UUID.randomUUID().toString();
    }
  }

  /**
   * Generates and applies a deterministic identifier to the given FHIR resource.
   *
   * <p>The identifier is computed from the resource's canonical URL, version, the package
   * coordinates from the manifest, and the content hash. Only {@link MetadataResource} instances
   * are processed; other resource types are ignored.
   *
   * @param resource the FHIR resource to assign the generated identifier to
   * @param resourceHash SHA-256 hash of the resource content, used to distinguish different
   *     versions of the same resource at identical coordinates
   * @param packageManifest manifest of the FHIR package the resource originates from
   */
  public static void applyNewId(
      IBaseResource resource, String resourceHash, PackageManifest packageManifest) {

    if (resource instanceof MetadataResource metadataResource) {
      String generatedId =
          generateId(
              metadataResource.getUrl(),
              metadataResource.getVersion(),
              packageManifest.getName(),
              packageManifest.getVersion(),
              resourceHash);
      metadataResource.setId(generatedId);
      log.debug(
          "Generated id for {}#{} : {}",
          metadataResource.getUrl(),
          metadataResource.getVersion(),
          generatedId);
    }
  }

  /**
   * Generates a deterministic version identifier and sets it on the resource's {@code
   * Meta.versionId}.
   *
   * <p>The identifier is derived from the same inputs as {@link #applyNewId(IBaseResource, String,
   * PackageManifest)}, but additionally incorporates a numeric version identifier. This allows
   * distinguishing successive versions of the same resource at identical coordinates. Only {@link
   * MetadataResource} instances are processed; other resource types are ignored.
   *
   * @param resource the FHIR resource whose {@code Meta.versionId} will be set
   * @param resourceHash SHA-256 hash of the resource content
   * @param packageManifest manifest of the FHIR package the resource originates from
   * @param versionId numeric version identifier to include in the UUID computation
   */
  public static void applyMetaVersionId(
      IBaseResource resource,
      String resourceHash,
      PackageManifest packageManifest,
      long versionId) {

    if (resource instanceof MetadataResource metadataResource) {
      String baseId =
          generateId(
              metadataResource.getUrl(),
              metadataResource.getVersion(),
              packageManifest.getName(),
              packageManifest.getVersion(),
              resourceHash);

      // Re-hash the base ID together with the version number
      String composedInput = baseId + "-" + versionId;
      String generatedId =
          uuidV5(NAMESPACE, composedInput.getBytes(StandardCharsets.UTF_8)).toString();

      metadataResource.getMeta().setVersionId(generatedId);
      log.debug(
          "Generated versioned id for {}#{} (versionId={}) : {}",
          metadataResource.getUrl(),
          metadataResource.getVersion(),
          versionId,
          generatedId);
    }
  }

  /**
   * Generates a name-based UUID version 5 (SHA-1) as defined by RFC 9562.
   *
   * @param namespace the namespace UUID (e.g. the well-known URL namespace from RFC 9562)
   * @param name the name bytes to hash within the namespace
   * @return a deterministic UUID derived from the namespace and name
   */
  @SuppressWarnings(
      "java:S4790") // We intentionally use SHA-1 here, as UUID v5 is defined to use SHA-1 according
  // to RFC 9562
  public static UUID uuidV5(UUID namespace, byte[] name) {
    // Build input: namespace UUID bytes followed by name bytes
    ByteBuffer nsBuffer = ByteBuffer.allocate(16);
    nsBuffer.putLong(namespace.getMostSignificantBits());
    nsBuffer.putLong(namespace.getLeastSignificantBits());

    byte[] input = new byte[16 + name.length];
    System.arraycopy(nsBuffer.array(), 0, input, 0, 16);
    System.arraycopy(name, 0, input, 16, name.length);

    byte[] hash = DigestUtils.sha1(input);

    // Set version 5
    hash[6] = (byte) ((hash[6] & 0x0F) | 0x50);
    // Set variant RFC 9562
    hash[8] = (byte) ((hash[8] & 0x3F) | 0x80);

    ByteBuffer buf = ByteBuffer.wrap(hash, 0, 16);
    return new UUID(buf.getLong(), buf.getLong());
  }
}
