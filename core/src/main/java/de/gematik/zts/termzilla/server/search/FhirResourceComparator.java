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
package de.gematik.zts.termzilla.server.search;

import static de.gematik.zts.termzilla.api.constants.FhirExtensionUrls.EXTENSION_RESOURCE_EFFECTIVE_PERIOD;

import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.exceptions.UnsupportedSortFieldException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.r4.model.ConceptMap.OtherElementComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;

/**
 * Builds comparators for FHIR resources based on search sort parameters.
 *
 * <p>This class implements FHIR sorting semantics including:
 *
 * <ul>
 *   <li>Multiple sort criteria in priority order
 *   <li>Ascending/descending order (indicated by '-' prefix)
 *   <li>Case-insensitive string comparison per FHIR specification
 *   <li>Null-safe comparisons (nulls sorted last)
 *   <li>Repeating elements: uses the earliest value in the specified sort order
 * </ul>
 *
 * <p>Example sort parameter: {@code "_sort=status,-date,name"}
 *
 * <ul>
 *   <li>Primary: status (ascending)
 *   <li>Secondary: date (descending)
 *   <li>Tertiary: name (ascending)
 * </ul>
 *
 * <p>Per FHIR specification: "A search parameter can refer to an element that repeats, and
 * therefore there can be multiple values for a given search parameter for a single resource. In
 * this case, the sort is based on the item in the set of multiple parameters that comes earliest in
 * the specified sort order when ordering the returned resources."
 */
@Slf4j
public class FhirResourceComparator {

  private FhirResourceComparator() {}

  /**
   * Builds a comparator chain from FHIR sort parameters.
   *
   * @param sortCriteria list of sort parameters (e.g., "status", "-date", "name")
   * @return combined comparator, or null if no sort criteria provided
   * @throws UnsupportedSortFieldException if any sort field is not supported
   */
  public static Comparator<IBaseResource> buildComparator(List<String> sortCriteria) {
    if (sortCriteria == null || sortCriteria.isEmpty()) {
      return null;
    }

    Comparator<IBaseResource> comparator = null;

    for (String criterion : sortCriteria) {
      boolean descending = criterion.startsWith("-");
      String fieldName = descending ? criterion.substring(1) : criterion;

      Comparator<IBaseResource> fieldComparator = createFieldComparator(fieldName, descending);

      if (descending) {
        fieldComparator = fieldComparator.reversed();
      }

      comparator =
          (comparator == null) ? fieldComparator : comparator.thenComparing(fieldComparator);
    }

    return comparator;
  }

  /**
   * Creates a comparator for a specific FHIR search parameter.
   *
   * @param fieldName the search parameter name
   * @param descending whether sorting is descending (affects which value to pick from repeating
   *     elements)
   * @return comparator for that field
   */
  private static Comparator<IBaseResource> createFieldComparator(
      String fieldName, boolean descending) {
    return (resource1, resource2) -> {
      // Handle null resources
      if (resource1 == null && resource2 == null) {
        return 0;
      }
      if (resource1 == null) {
        return 1;
      }
      if (resource2 == null) {
        return -1;
      }

      // Extract values based on field name and resource type
      Object value1 = extractFieldValue(resource1, fieldName, descending);
      Object value2 = extractFieldValue(resource2, fieldName, descending);

      return compareValues(value1, value2);
    };
  }

  /**
   * Extracts a field value from a FHIR resource.
   *
   * <p>For repeating elements, returns the value that comes earliest in the specified sort order.
   * For ascending order, this is the minimum value; for descending order, the maximum value.
   *
   * @param resource the FHIR resource
   * @param fieldName the search parameter name
   * @param descending whether sorting is descending
   * @return the extracted value, or null if not present
   * @throws UnsupportedSortFieldException if the field is not supported for the resource type
   */
  private static Object extractFieldValue(
      IBaseResource resource, String fieldName, boolean descending) {

    // Try common parameters first (work for all resource types)
    Object commonValue = extractCommonField(resource, fieldName, descending);
    if (commonValue != null || isCommonField(fieldName, resource)) {
      return commonValue;
    }

    // Fall back to resource-specific extraction
    if (resource instanceof CodeSystem cs) {
      return extractCodeSystemField(cs, fieldName, descending);
    } else if (resource instanceof ValueSet vs) {
      return extractValueSetField(vs, fieldName, descending);
    } else if (resource instanceof ConceptMap cm) {
      return extractConceptMapField(cm, fieldName, descending);
    }

    throw new UnsupportedSortFieldException(
        "Unsupported sort field '"
            + fieldName
            + "' for resource type: "
            + resource.getClass().getSimpleName());
  }

  /**
   * Checks if a field name is a common field that applies to the given resource.
   *
   * @param fieldName the field name to check
   * @param resource the resource
   * @return true if the field is a common field for this resource type
   */
  private static boolean isCommonField(String fieldName, IBaseResource resource) {
    // Base resource fields (all resources)
    if (TerminologyResourceSearchParameter.SP_ID.equals(fieldName)
        || TerminologyResourceSearchParameter.SP_LAST_UPDATED.equals(fieldName)
        || TerminologyResourceSearchParameter.SP_TAG.equals(fieldName)
        || TerminologyResourceSearchParameter.SP_PROFILE.equals(fieldName)
        || TerminologyResourceSearchParameter.SP_SECURITY.equals(fieldName)
        || TerminologyResourceSearchParameter.SP_SOURCE.equals(fieldName)) {
      return true;
    }

    // MetadataResource fields
    if (resource instanceof MetadataResource) {
      return TerminologyResourceSearchParameter.SP_URL.equals(fieldName)
          || TerminologyResourceSearchParameter.SP_VERSION.equals(fieldName)
          || TerminologyResourceSearchParameter.SP_NAME.equals(fieldName)
          || TerminologyResourceSearchParameter.SP_TITLE.equals(fieldName)
          || TerminologyResourceSearchParameter.SP_STATUS.equals(fieldName)
          || TerminologyResourceSearchParameter.SP_DATE.equals(fieldName)
          || TerminologyResourceSearchParameter.SP_PUBLISHER.equals(fieldName)
          || TerminologyResourceSearchParameter.SP_DESCRIPTION.equals(fieldName)
          || TerminologyResourceSearchParameter.SP_JURISDICTION.equals(fieldName)
          || TerminologyResourceSearchParameter.SP_CONTENT.equals(fieldName)
          || TerminologyResourceSearchParameter.SP_EFFECTIVE.equals(fieldName);
    }

    return false;
  }

  /**
   * Extracts common field values that apply to all or most FHIR resources.
   *
   * <p>Common search parameters include: _id, _lastUpdated, _tag, _profile, _security, and metadata
   * resource fields like url, version, name, title, status, date, publisher, description.
   *
   * @param resource the FHIR resource
   * @param fieldName the search parameter name
   * @param descending whether sorting is descending
   * @return the field value, or null if not a common field or not present
   */
  private static Object extractCommonField(
      IBaseResource resource, String fieldName, boolean descending) {

    // Base resource fields (all resources)
    switch (fieldName) {
      case TerminologyResourceSearchParameter.SP_ID:
        IIdType idElement = resource.getIdElement();
        return idElement != null ? idElement.getIdPart() : null;

      case TerminologyResourceSearchParameter.SP_LAST_UPDATED:
        return extractLastUpdated(resource);

      case TerminologyResourceSearchParameter.SP_TAG:
        return extractEarliestTag(resource, descending);

      case TerminologyResourceSearchParameter.SP_PROFILE:
        return extractEarliestProfile(resource, descending);

      case TerminologyResourceSearchParameter.SP_SECURITY:
        return extractEarliestSecurity(resource, descending);

      case TerminologyResourceSearchParameter.SP_SOURCE:
        return extractSource(resource);

      default:
        // Not a common field, will try resource-specific extraction next
    }

    // MetadataResource fields (CodeSystem, ValueSet, ConceptMap, etc.)
    if (resource instanceof MetadataResource metadataResource) {
      return switch (fieldName) {
        case TerminologyResourceSearchParameter.SP_URL -> metadataResource.getUrl();
        case TerminologyResourceSearchParameter.SP_VERSION -> metadataResource.getVersion();
        case TerminologyResourceSearchParameter.SP_NAME -> metadataResource.getName();
        case TerminologyResourceSearchParameter.SP_TITLE -> metadataResource.getTitle();
        case TerminologyResourceSearchParameter.SP_STATUS ->
            metadataResource.hasStatus() ? metadataResource.getStatus().toCode() : null;
        case TerminologyResourceSearchParameter.SP_DATE -> metadataResource.getDate();
        case TerminologyResourceSearchParameter.SP_PUBLISHER -> metadataResource.getPublisher();
        case TerminologyResourceSearchParameter.SP_DESCRIPTION -> metadataResource.getDescription();
        case TerminologyResourceSearchParameter.SP_JURISDICTION ->
            extractEarliestJurisdiction(metadataResource.getJurisdiction(), descending);
        case TerminologyResourceSearchParameter.SP_CONTENT ->
            extractEarliestUsageContext(metadataResource.getUseContext(), descending);
        case TerminologyResourceSearchParameter.SP_EFFECTIVE ->
            extractEffectivePeriod(metadataResource);

        default -> null;
      };
    }

    return null;
  }

  /**
   * Extracts the earliest identifier in system|value format.
   *
   * <p>Format: "system|value" or "value" if system is not present. Identifiers without a value are
   * excluded.
   *
   * @param identifiers list of identifiers
   * @param descending whether sorting is descending
   * @return the earliest identifier in system|value format, or null if list is empty
   */
  private static String extractEarliestIdentifier(
      List<Identifier> identifiers, boolean descending) {
    if (identifiers == null || identifiers.isEmpty()) {
      return null;
    }

    Stream<String> values =
        identifiers.stream()
            .filter(Identifier::hasValue)
            .map(
                identifier -> {
                  String system = identifier.hasSystem() ? identifier.getSystem() : "";
                  String value = identifier.getValue();
                  return system.isEmpty() ? value : system + "|" + value;
                });

    return descending
        ? values.max(String.CASE_INSENSITIVE_ORDER).orElse(null)
        : values.min(String.CASE_INSENSITIVE_ORDER).orElse(null);
  }

  /**
   * Extracts the earliest tag from resource metadata in system|code format.
   *
   * <p>Format: "system|code" or "code" if system is not present.
   *
   * @param resource the FHIR resource
   * @param descending whether sorting is descending
   * @return the earliest tag in system|code format, or null if none present
   */
  private static String extractEarliestTag(IBaseResource resource, boolean descending) {
    if (!(resource instanceof Resource r4Resource)) {
      return null;
    }

    Meta meta = r4Resource.getMeta();
    if (meta == null || !meta.hasTag()) {
      return null;
    }

    Stream<String> tags =
        meta.getTag().stream()
            .filter(Coding::hasCode)
            .map(
                coding -> {
                  String system = coding.hasSystem() ? coding.getSystem() : "";
                  String code = coding.getCode();
                  return system.isEmpty() ? code : system + "|" + code;
                });

    return descending
        ? tags.max(String.CASE_INSENSITIVE_ORDER).orElse(null)
        : tags.min(String.CASE_INSENSITIVE_ORDER).orElse(null);
  }

  /**
   * Extracts the earliest security label from resource metadata in system|code format.
   *
   * <p>Format: "system|code" or "code" if system is not present.
   *
   * @param resource the FHIR resource
   * @param descending whether sorting is descending
   * @return the earliest security code in system|code format, or null if none present
   */
  private static String extractEarliestSecurity(IBaseResource resource, boolean descending) {
    if (!(resource instanceof Resource r4Resource)) {
      return null;
    }

    Meta meta = r4Resource.getMeta();
    if (meta == null || !meta.hasSecurity()) {
      return null;
    }

    Stream<String> security =
        meta.getSecurity().stream()
            .filter(Coding::hasCode)
            .map(
                coding -> {
                  String system = coding.hasSystem() ? coding.getSystem() : "";
                  String code = coding.getCode();
                  return system.isEmpty() ? code : system + "|" + code;
                });

    return descending
        ? security.max(String.CASE_INSENSITIVE_ORDER).orElse(null)
        : security.min(String.CASE_INSENSITIVE_ORDER).orElse(null);
  }

  /**
   * Extracts the earliest jurisdiction code from a list of codeable concepts in system|code format.
   *
   * <p>Format: "system|code" or "code" if system is not present.
   *
   * @param jurisdictions list of jurisdiction codeable concepts
   * @param descending whether sorting is descending
   * @return the earliest jurisdiction code in system|code format, or null if list is empty
   */
  private static String extractEarliestJurisdiction(
      List<CodeableConcept> jurisdictions, boolean descending) {
    if (jurisdictions == null || jurisdictions.isEmpty()) {
      return null;
    }

    Stream<String> codes =
        jurisdictions.stream()
            .flatMap(cc -> cc.getCoding().stream())
            .filter(Coding::hasCode)
            .map(
                coding -> {
                  String system = coding.hasSystem() ? coding.getSystem() : "";
                  String code = coding.getCode();
                  return system.isEmpty() ? code : system + "|" + code;
                });

    return descending
        ? codes.max(String.CASE_INSENSITIVE_ORDER).orElse(null)
        : codes.min(String.CASE_INSENSITIVE_ORDER).orElse(null);
  }

  /**
   * Extracts the earliest usage context value from a list of usage contexts in system|code format.
   *
   * <p>Format: "system|code" or "code" if system is not present.
   *
   * @param contexts list of usage contexts
   * @param descending whether sorting is descending
   * @return the earliest context value in system|code format, or null if list is empty
   */
  private static String extractEarliestUsageContext(
      List<UsageContext> contexts, boolean descending) {
    if (contexts == null || contexts.isEmpty()) {
      return null;
    }

    Stream<String> values =
        contexts.stream()
            .filter(UsageContext::hasValueCodeableConcept)
            .flatMap(uc -> uc.getValueCodeableConcept().getCoding().stream())
            .filter(Coding::hasCode)
            .map(
                coding -> {
                  String system = coding.hasSystem() ? coding.getSystem() : "";
                  String code = coding.getCode();
                  return system.isEmpty() ? code : system + "|" + code;
                });

    return descending
        ? values.max(String.CASE_INSENSITIVE_ORDER).orElse(null)
        : values.min(String.CASE_INSENSITIVE_ORDER).orElse(null);
  }

  /**
   * Extracts the effective period date from a metadata resource.
   *
   * <p>Looks for the resource-effectivePeriod extension and returns the start date if present,
   * otherwise the end date. Returns null if the extension is not present or contains no dates.
   *
   * @param metadataResource the metadata resource
   * @return the effective date (start or end), or null if not present
   */
  private static Date extractEffectivePeriod(MetadataResource metadataResource) {
    if (!metadataResource.hasExtension(EXTENSION_RESOURCE_EFFECTIVE_PERIOD)) {
      return null;
    }

    Extension ext = metadataResource.getExtensionByUrl(EXTENSION_RESOURCE_EFFECTIVE_PERIOD);
    if (ext == null || !ext.hasValue() || !(ext.getValue() instanceof Period period)) {
      return null;
    }

    // Use start date if available, otherwise end date
    if (period.hasStart()) {
      return period.getStart();
    } else if (period.hasEnd()) {
      return period.getEnd();
    }

    return null;
  }

  /**
   * Extracts the lastUpdated timestamp from a resource's metadata.
   *
   * @param resource the FHIR resource
   * @return the lastUpdated date, or null if not present
   */
  private static Date extractLastUpdated(IBaseResource resource) {
    // For R4 resources, cast to Resource to get Meta
    if (resource instanceof Resource r4Resource) {
      Meta meta = r4Resource.getMeta();
      if (meta != null && meta.hasLastUpdated()) {
        return meta.getLastUpdated();
      }
    }
    return null;
  }

  /**
   * Extracts the source URI from a resource's metadata.
   *
   * @param resource the FHIR resource
   * @return the source URI, or null if not present
   */
  private static String extractSource(IBaseResource resource) {
    if (resource instanceof Resource r4Resource) {
      Meta meta = r4Resource.getMeta();
      if (meta != null && meta.hasSource()) {
        return meta.getSource();
      }
    }
    return null;
  }

  /**
   * Extracts the earliest profile from resource metadata.
   *
   * @param resource the FHIR resource
   * @param descending whether sorting is descending
   * @return the earliest profile URL, or null if none present
   */
  private static String extractEarliestProfile(IBaseResource resource, boolean descending) {
    if (!(resource instanceof Resource r4Resource)) {
      return null;
    }

    Meta meta = r4Resource.getMeta();
    if (meta == null || !meta.hasProfile()) {
      return null;
    }

    Stream<String> profiles = meta.getProfile().stream().map(PrimitiveType::getValue);

    return descending
        ? profiles.max(String.CASE_INSENSITIVE_ORDER).orElse(null)
        : profiles.min(String.CASE_INSENSITIVE_ORDER).orElse(null);
  }

  /**
   * Extracts a field value from a CodeSystem resource.
   *
   * <p>Only handles CodeSystem-specific fields. Common fields are handled by {@link
   * #extractCommonField}.
   *
   * @param cs the CodeSystem
   * @param fieldName the field name
   * @param descending whether sorting is descending
   * @return the field value
   * @throws UnsupportedSortFieldException if the field is not supported for CodeSystem
   */
  private static Object extractCodeSystemField(
      CodeSystem cs, String fieldName, boolean descending) {
    return switch (fieldName) {
      case CsSearchParameter.SP_CONTENT_MODE -> cs.hasContent() ? cs.getContent().toCode() : null;
      case CsSearchParameter.SP_SUPPLEMENTS -> cs.getSupplements();
      case CsSearchParameter.SP_SYSTEM ->
          cs.getUrl(); // 'system' search parameter maps to url for CodeSystem
      case TerminologyResourceSearchParameter.SP_IDENTIFIER ->
          extractEarliestIdentifier(cs.getIdentifier(), descending);
      default ->
          throw new UnsupportedSortFieldException(
              "Unsupported sort field '" + fieldName + "' for CodeSystem");
    };
  }

  /**
   * Extracts a field value from a ValueSet resource.
   *
   * <p>Only handles ValueSet-specific fields. Common fields are handled by {@link
   * #extractCommonField}.
   *
   * @param vs the ValueSet
   * @param fieldName the field name
   * @param descending whether sorting is descending
   * @return the field value
   * @throws UnsupportedSortFieldException if the field is not supported for ValueSet
   */
  private static Object extractValueSetField(ValueSet vs, String fieldName, boolean descending) {
    return switch (fieldName) {
      case TerminologyResourceSearchParameter.SP_IDENTIFIER ->
          extractEarliestIdentifier(vs.getIdentifier(), descending);
      case VsSearchParameter.SP_REFERENCE -> extractEarliestReference(vs, descending);
      case VsSearchParameter.SP_EXPANSION ->
          vs.hasExpansion() ? vs.getExpansion().getIdentifier() : null;
      default ->
          throw new UnsupportedSortFieldException(
              "Unsupported sort field '" + fieldName + "' for ValueSet");
    };
  }

  /**
   * Extracts a field value from a ConceptMap resource.
   *
   * <p>Only handles ConceptMap-specific fields. Common fields are handled by {@link
   * #extractCommonField}.
   *
   * @param cm the ConceptMap
   * @param fieldName the field name
   * @param descending whether sorting is descending
   * @return the field value
   * @throws UnsupportedSortFieldException if the field is not supported for ConceptMap
   */
  private static Object extractConceptMapField(
      ConceptMap cm, String fieldName, boolean descending) {
    return switch (fieldName) {
      // ConceptMap can have source/target as either canonical or uri
      case CmSearchParameter.SP_SOURCE_CANONICAL, CmSearchParameter.SP_SOURCE_URI -> {
        if (cm.hasSourceCanonicalType()) {
          yield cm.getSourceCanonicalType().getValue();
        } else if (cm.hasSourceUriType()) {
          yield cm.getSourceUriType().getValue();
        }
        yield null;
      }
      case CmSearchParameter.SP_TARGET_CANONICAL, CmSearchParameter.SP_TARGET_URI -> {
        if (cm.hasTargetCanonicalType()) {
          yield cm.getTargetCanonicalType().getValue();
        } else if (cm.hasTargetUriType()) {
          yield cm.getTargetUriType().getValue();
        }
        yield null;
      }
      // ConceptMap has a single identifier, not a list
      case TerminologyResourceSearchParameter.SP_IDENTIFIER ->
          cm.hasIdentifier() ? cm.getIdentifier().getValue() : null;
      case CmSearchParameter.SP_SOURCE_SYSTEM -> extractEarliestSourceSystem(cm, descending);
      case CmSearchParameter.SP_TARGET_SYSTEM -> extractEarliestTargetSystem(cm, descending);
      case CmSearchParameter.SP_DEPENDS_ON -> extractEarliestDependsOn(cm, descending);
      case CmSearchParameter.SP_PRODUCT -> extractEarliestProduct(cm, descending);
      default ->
          throw new UnsupportedSortFieldException(
              "Unsupported sort field '" + fieldName + "' for ConceptMap");
    };
  }

  /**
   * Extracts the earliest reference from a ValueSet's compose include/exclude.
   *
   * @param vs the ValueSet
   * @param descending whether sorting is descending
   * @return the earliest reference, or null if none present
   */
  private static String extractEarliestReference(ValueSet vs, boolean descending) {
    if (!vs.hasCompose()) {
      return null;
    }

    Stream<String> systems =
        Stream.concat(vs.getCompose().getInclude().stream(), vs.getCompose().getExclude().stream())
            .filter(ConceptSetComponent::hasSystem)
            .map(ConceptSetComponent::getSystem);

    return descending
        ? systems.max(String.CASE_INSENSITIVE_ORDER).orElse(null)
        : systems.min(String.CASE_INSENSITIVE_ORDER).orElse(null);
  }

  /**
   * Extracts the earliest source system from a ConceptMap.
   *
   * @param cm the ConceptMap
   * @param descending whether sorting is descending
   * @return the earliest source system, or null if none present
   */
  private static String extractEarliestSourceSystem(ConceptMap cm, boolean descending) {
    if (!cm.hasGroup()) {
      return null;
    }

    Stream<String> systems =
        cm.getGroup().stream()
            .filter(ConceptMapGroupComponent::hasSource)
            .map(ConceptMapGroupComponent::getSource);

    return descending
        ? systems.max(String.CASE_INSENSITIVE_ORDER).orElse(null)
        : systems.min(String.CASE_INSENSITIVE_ORDER).orElse(null);
  }

  /**
   * Extracts the earliest target system from a ConceptMap.
   *
   * @param cm the ConceptMap
   * @param descending whether sorting is descending
   * @return the earliest target system, or null if none present
   */
  private static String extractEarliestTargetSystem(ConceptMap cm, boolean descending) {
    if (!cm.hasGroup()) {
      return null;
    }

    Stream<String> systems =
        cm.getGroup().stream()
            .filter(ConceptMapGroupComponent::hasTarget)
            .map(ConceptMapGroupComponent::getTarget);

    return descending
        ? systems.max(String.CASE_INSENSITIVE_ORDER).orElse(null)
        : systems.min(String.CASE_INSENSITIVE_ORDER).orElse(null);
  }

  /**
   * Extracts the earliest dependsOn property from a ConceptMap.
   *
   * @param cm the ConceptMap
   * @param descending whether sorting is descending
   * @return the earliest dependsOn property, or null if none present
   */
  private static String extractEarliestDependsOn(ConceptMap cm, boolean descending) {
    if (!cm.hasGroup()) {
      return null;
    }

    Stream<String> properties =
        cm.getGroup().stream()
            .flatMap(group -> group.getElement().stream())
            .flatMap(element -> element.getTarget().stream())
            .flatMap(target -> target.getDependsOn().stream())
            .filter(OtherElementComponent::hasProperty)
            .map(OtherElementComponent::getProperty);

    return descending
        ? properties.max(String.CASE_INSENSITIVE_ORDER).orElse(null)
        : properties.min(String.CASE_INSENSITIVE_ORDER).orElse(null);
  }

  /**
   * Extracts the earliest product property from a ConceptMap.
   *
   * @param cm the ConceptMap
   * @param descending whether sorting is descending
   * @return the earliest product property, or null if none present
   */
  private static String extractEarliestProduct(ConceptMap cm, boolean descending) {
    if (!cm.hasGroup()) {
      return null;
    }

    Stream<String> properties =
        cm.getGroup().stream()
            .flatMap(group -> group.getElement().stream())
            .flatMap(element -> element.getTarget().stream())
            .flatMap(target -> target.getProduct().stream())
            .filter(OtherElementComponent::hasProperty)
            .map(OtherElementComponent::getProperty);

    return descending
        ? properties.max(String.CASE_INSENSITIVE_ORDER).orElse(null)
        : properties.min(String.CASE_INSENSITIVE_ORDER).orElse(null);
  }

  /**
   * Compares two values with type-specific logic.
   *
   * @param value1 first value
   * @param value2 second value
   * @return comparison result
   */
  private static int compareValues(Object value1, Object value2) {
    // Handle nulls (nulls last per FHIR specification)
    if (value1 == null && value2 == null) {
      return 0;
    }
    if (value1 == null) {
      return 1;
    }
    if (value2 == null) {
      return -1;
    }

    // Date comparison
    switch (value1) {
      case Date date1 when value2 instanceof Date date2 -> {
        return date1.compareTo(date2);
      }

      // Number comparison
      case Number num1 when value2 instanceof Number num2 -> {
        return Double.compare(num1.doubleValue(), num2.doubleValue());
      }

      // Boolean comparison
      case Boolean bool1 when value2 instanceof Boolean bool2 -> {
        return bool1.compareTo(bool2);
      }
      default -> {
        // Fall back to string comparison for all other types (including String, Enum, etc.)
      }
    }

    // String comparison (case-insensitive per FHIR specification)
    String str1 = value1.toString();
    String str2 = value2.toString();

    return str1.compareToIgnoreCase(str2);
  }
}
