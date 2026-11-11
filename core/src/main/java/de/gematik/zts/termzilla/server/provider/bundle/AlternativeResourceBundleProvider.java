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
package de.gematik.zts.termzilla.server.provider.bundle;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.InstantType;

/**
 * Generic implementation of {@link IBundleProvider} for FHIR resource search operations.
 *
 * <p>This class provides a reusable implementation of the bundle provider pattern used for
 * paginated search results in FHIR REST operations. It handles the retrieval of resources based on
 * a list of resource IDs and a resource loader function.
 *
 * <p>Pagination is controlled by optional {@code offset} and {@code count} parameters. The
 * effective page window is determined with the following priority:
 *
 * <ul>
 *   <li><b>offset:</b> explicit value if provided, otherwise {@code theFromIndex}
 *   <li><b>count:</b> explicit value if provided, otherwise {@code preferredPageSize}, otherwise
 *       {@code theToIndex - theFromIndex}
 * </ul>
 *
 * <p>The resulting range is always clamped to the total number of available resources.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * List<IdType> matchingIds = terminologyService.searchCodeSystem(...);
 * return new AlternativeResourceBundleProvider(
 *     matchingIds,
 *     id -> terminologyService.getCodeSystem(tenantId, id),
 *     offset,
 *     count
 * );
 * }</pre>
 *
 * @see IBundleProvider
 */
public class AlternativeResourceBundleProvider implements IBundleProvider {

  private final List<IdType> resourceIds;
  private final Function<IdType, IBaseResource> resourceLoader;
  private final InstantType searchTime;
  private final Integer preferredPageSize;
  private final Integer offset;
  private final Integer count;

  /**
   * Creates a new bundle provider with a default preferred page size of 50.
   *
   * @param resourceIds list of resource IDs that match the search criteria
   * @param resourceLoader function to load a resource by its ID
   * @param offset zero-based start index into the result set, or {@code null} to use the
   *     framework-provided index
   * @param count maximum number of resources to return per page, or {@code null} to fall back to
   *     the preferred page size
   */
  public AlternativeResourceBundleProvider(
      List<IdType> resourceIds,
      Function<IdType, IBaseResource> resourceLoader,
      Integer offset,
      Integer count) {
    this(resourceIds, resourceLoader, 50, offset, count);
  }

  /**
   * Creates a new bundle provider with a custom preferred page size.
   *
   * @param resourceIds list of resource IDs that match the search criteria
   * @param resourceLoader function to load a resource by its ID
   * @param preferredPageSize preferred number of resources per page, used as fallback when {@code
   *     count} is {@code null}; may itself be {@code null} for no preference
   * @param offset zero-based start index into the result set, or {@code null} to use the
   *     framework-provided index
   * @param count maximum number of resources to return per page, or {@code null} to fall back to
   *     {@code preferredPageSize}
   */
  public AlternativeResourceBundleProvider(
      List<IdType> resourceIds,
      Function<IdType, IBaseResource> resourceLoader,
      Integer preferredPageSize,
      Integer offset,
      Integer count) {
    this.resourceIds = resourceIds;
    this.resourceLoader = resourceLoader;
    this.searchTime = InstantType.withCurrentTime();
    this.preferredPageSize = preferredPageSize;
    this.offset = offset;
    this.count = count;
  }

  @Override
  public Integer size() {
    return resourceIds.size();
  }

  @Nonnull
  @Override
  public List<IBaseResource> getResources(int theFromIndex, int theToIndex) {
    int totalSize = resourceIds.size();

    int effectiveOffset = offset != null ? offset : theFromIndex;
    int fallbackCount = preferredPageSize != null ? preferredPageSize : (theToIndex - theFromIndex);
    int effectiveCount = count != null ? count : fallbackCount;

    int from = Math.min(effectiveOffset, totalSize);
    int to = Math.min(from + effectiveCount, totalSize);

    List<IBaseResource> resources = new ArrayList<>();
    for (IdType id : resourceIds.subList(from, to)) {
      resources.add(resourceLoader.apply(id));
    }

    return resources;
  }

  @Override
  public InstantType getPublished() {
    return searchTime;
  }

  @Override
  public Integer preferredPageSize() {
    return preferredPageSize;
  }

  @Override
  public String getUuid() {
    return null;
  }
}
