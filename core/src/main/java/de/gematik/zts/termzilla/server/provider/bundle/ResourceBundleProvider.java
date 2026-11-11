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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
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
 * <p>Typical usage:
 *
 * <pre>{@code
 * List<IdType> matchingIds = terminologyService.searchCodeSystem(...);
 * return new ResourceBundleProvider(
 *     matchingIds,
 *     id -> terminologyService.getCodeSystem(tenantId, id)
 * );
 * }</pre>
 *
 * @see IBundleProvider
 */
public class ResourceBundleProvider implements IBundleProvider {

  private final List<IdType> resourceIds;
  private final Function<IdType, IBaseResource> resourceLoader;
  private final InstantType searchTime;
  private final Integer preferredPageSize;

  /**
   * Creates a new ResourceBundleProvider with default page size of 10.
   *
   * @param resourceIds List of resource IDs that match the search criteria
   * @param resourceLoader Function to load a resource by its ID
   */
  public ResourceBundleProvider(
      List<IdType> resourceIds, Function<IdType, IBaseResource> resourceLoader) {
    this(resourceIds, resourceLoader, 10);
  }

  /**
   * Creates a new ResourceBundleProvider with custom page size.
   *
   * @param resourceIds List of resource IDs that match the search criteria
   * @param resourceLoader Function to load a resource by its ID
   * @param preferredPageSize Preferred number of resources per page (null for no preference)
   */
  public ResourceBundleProvider(
      List<IdType> resourceIds,
      Function<IdType, IBaseResource> resourceLoader,
      Integer preferredPageSize) {
    this.resourceIds = resourceIds;
    this.resourceLoader = resourceLoader;
    this.searchTime = InstantType.withCurrentTime();
    this.preferredPageSize = preferredPageSize;
  }

  @Override
  public Integer size() {
    return resourceIds.size();
  }

  @Nonnull
  @Override
  public List<IBaseResource> getResources(int theFromIndex, int theToIndex) {
    int end = Math.min(theToIndex, resourceIds.size());
    List<IdType> idsToReturn = resourceIds.subList(theFromIndex, end);

    List<IBaseResource> resources = new ArrayList<>();
    for (IdType id : idsToReturn) {
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
