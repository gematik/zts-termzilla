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
package de.gematik.zts.termzilla.server.service.fixtures;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Builder for creating test FHIR resources and search parameters.
 *
 * <p>Provides convenient methods to create FHIR resources and parameter objects for testing without
 * boilerplate code.
 */
public class TestDataBuilder {

  // ==================================================================================================
  // CodeSystem Resources
  // ==================================================================================================

  /**
   * Creates a minimal CodeSystem with the specified URL and codes.
   *
   * @param url the canonical URL of the CodeSystem
   * @param codes list of concept codes to include
   * @return a configured CodeSystem resource
   */
  public static CodeSystem createCodeSystem(String url, List<String> codes) {
    return createCodeSystem(url, url, codes, PublicationStatus.ACTIVE);
  }

  /**
   * Creates a CodeSystem with full configuration.
   *
   * @param url the canonical URL
   * @param name the name of the CodeSystem
   * @param codes list of concept codes
   * @param status publication status
   * @return a configured CodeSystem resource
   */
  public static CodeSystem createCodeSystem(
      String url, String name, List<String> codes, PublicationStatus status) {
    CodeSystem cs = new CodeSystem();
    cs.setId(extractIdFromUrl(url));
    cs.setUrl(url);
    cs.setName(name);
    cs.setStatus(status);
    cs.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
    cs.setVersion("1.0.0");
    cs.setDate(new Date());

    for (String code : codes) {
      CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
      concept.setCode(code);
      concept.setDisplay("Display for " + code);
      cs.addConcept(concept);
    }

    return cs;
  }

  // ==================================================================================================
  // ValueSet Resources
  // ==================================================================================================

  /**
   * Creates a minimal ValueSet that includes all codes from a CodeSystem.
   *
   * @param url the canonical URL of the ValueSet
   * @param codeSystemUrl the CodeSystem to include
   * @return a configured ValueSet resource
   */
  public static ValueSet createValueSet(String url, String codeSystemUrl) {
    return createValueSet(url, url, codeSystemUrl, PublicationStatus.ACTIVE);
  }

  /**
   * Creates a ValueSet with full configuration.
   *
   * @param url the canonical URL
   * @param name the name
   * @param codeSystemUrl the CodeSystem to include
   * @param status publication status
   * @return a configured ValueSet resource
   */
  public static ValueSet createValueSet(
      String url, String name, String codeSystemUrl, PublicationStatus status) {
    ValueSet vs = new ValueSet();
    vs.setId(extractIdFromUrl(url));
    vs.setUrl(url);
    vs.setName(name);
    vs.setStatus(status);
    vs.setVersion("1.0.0");
    vs.setDate(new Date());

    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.setSystem(codeSystemUrl);
    vs.getCompose().addInclude(include);

    return vs;
  }

  /**
   * Creates a ValueSet with specific codes included.
   *
   * @param url the canonical URL
   * @param codeSystemUrl the CodeSystem URL
   * @param codes specific codes to include
   * @return a configured ValueSet resource
   */
  public static ValueSet createValueSetWithCodes(
      String url, String codeSystemUrl, List<String> codes) {
    ValueSet vs = createValueSet(url, codeSystemUrl);

    ValueSet.ConceptSetComponent include = vs.getCompose().getIncludeFirstRep();
    for (String code : codes) {
      ValueSet.ConceptReferenceComponent concept = new ValueSet.ConceptReferenceComponent();
      concept.setCode(code);
      include.addConcept(concept);
    }

    return vs;
  }

  /**
   * Creates an expanded ValueSet with the given concepts.
   *
   * @param url the canonical URL
   * @param codes list of codes in the expansion
   * @return a ValueSet with expansion
   */
  public static ValueSet createExpandedValueSet(String url, List<String> codes) {
    ValueSet vs = new ValueSet();
    vs.setUrl(url);

    ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
    expansion.setTimestamp(new Date());
    expansion.setTotal(codes.size());

    for (String code : codes) {
      ValueSet.ValueSetExpansionContainsComponent contains =
          new ValueSet.ValueSetExpansionContainsComponent();
      contains.setCode(code);
      contains.setDisplay("Display for " + code);
      expansion.addContains(contains);
    }

    vs.setExpansion(expansion);
    return vs;
  }

  // ==================================================================================================
  // ConceptMap Resources
  // ==================================================================================================

  /**
   * Creates a minimal ConceptMap.
   *
   * @param url the canonical URL
   * @param sourceUrl source CodeSystem URL
   * @param targetUrl target CodeSystem URL
   * @return a configured ConceptMap resource
   */
  public static ConceptMap createConceptMap(String url, String sourceUrl, String targetUrl) {
    ConceptMap cm = new ConceptMap();
    cm.setId(extractIdFromUrl(url));
    cm.setUrl(url);
    cm.setName(extractIdFromUrl(url));
    cm.setStatus(PublicationStatus.ACTIVE);
    cm.setVersion("1.0.0");
    cm.setDate(new Date());
    cm.setSource(new CanonicalType(sourceUrl));
    cm.setTarget(new CanonicalType(targetUrl));

    return cm;
  }

  // ==================================================================================================
  // Search Parameters
  // ==================================================================================================

  /**
   * Creates a CodeSystem search parameter with URL filter.
   *
   * @param url the URL to search for
   * @return configured search parameter
   */
  public static CsSearchParameter createCsSearchParameter(String url) {

    CsSearchParameter parameter = CsSearchParameter.builder().build();
    parameter.setUrlParam(new UriParam(url));
    return parameter;
  }

  /**
   * Creates a CodeSystem search parameter with name filter.
   *
   * @param name the name to search for
   * @return configured search parameter
   */
  public static CsSearchParameter createCsSearchParameterByName(String name) {
    CsSearchParameter parameter = CsSearchParameter.builder().build();
    parameter.setNameParam(new StringParam(name));
    return parameter;
  }

  /**
   * Creates a ValueSet search parameter with URL filter.
   *
   * @param url the URL to search for
   * @return configured search parameter
   */
  public static VsSearchParameter createVsSearchParameter(String url) {
    VsSearchParameter parameter = VsSearchParameter.builder().build();
    parameter.setUrlParam(new UriParam(url));
    return parameter;
  }

  /**
   * Creates a ConceptMap search parameter with URL filter.
   *
   * @param url the URL to search for
   * @return configured search parameter
   */
  public static CmSearchParameter createCmSearchParameter(String url) {
    CmSearchParameter parameter = CmSearchParameter.builder().build();
    parameter.setUrlParam(new UriParam(url));
    return parameter;
  }

  // ==================================================================================================
  // Operation Parameters
  // ==================================================================================================

  /**
   * Creates a lookup parameter for CodeSystem $lookup operation.
   *
   * @param system the CodeSystem URL
   * @param code the code to look up
   * @return configured lookup parameter
   */
  public static CsLookupParameter createLookupParameter(String system, String code) {
    return CsLookupParameter.builder()
        .codeParam(new CodeType(code))
        .systemParam(new UriType(system))
        .build();
  }

  /**
   * Creates a validate-code parameter for CodeSystem $validate-code operation.
   *
   * @param url the CodeSystem URL
   * @param code the code to validate
   * @return configured validation parameter
   */
  public static CsValidateCodeParameter createValidateCodeParameter(String url, String code) {
    return CsValidateCodeParameter.builder()
        .urlParam(new UriType(url))
        .codeParam(new CodeType(code))
        .build();
  }

  /**
   * Creates an expand parameter for ValueSet $expand operation.
   *
   * @param valueSet the ValueSet to expand
   * @return configured expand parameter
   */
  public static VsExpandParameter createExpandParameter(ValueSet valueSet) {
    return VsExpandParameter.builder().valueSetParam(valueSet).build();
  }

  /**
   * Creates an expand parameter with URL.
   *
   * @param url the ValueSet URL to expand
   * @return configured expand parameter
   */
  public static VsExpandParameter createExpandParameterByUrl(String url) {
    return VsExpandParameter.builder().urlParam(new UriType(url)).build();
  }

  /**
   * Creates a validate-code parameter for ValueSet $validate-code operation.
   *
   * @param url the ValueSet URL
   * @param code the code to validate
   * @return configured validation parameter
   */
  public static VsValidateCodeParameter createVsValidateCodeParameter(String url, String code) {
    return VsValidateCodeParameter.builder()
        .urlParam(new UriParam(url))
        .codeParam(new TokenParam(code))
        .build();
  }

  // ==================================================================================================
  // Helper Methods
  // ==================================================================================================

  /**
   * Extracts a simple ID from a canonical URL.
   *
   * @param url the canonical URL
   * @return the extracted ID (last path segment)
   */
  private static String extractIdFromUrl(String url) {
    String[] parts = url.split("/");
    return parts[parts.length - 1];
  }

  /**
   * Creates a list of IdType from string IDs.
   *
   * @param resourceType the resource type (e.g., "CodeSystem")
   * @param ids list of ID strings
   * @return list of IdType instances
   */
  public static List<IdType> createIdList(String resourceType, List<String> ids) {
    List<IdType> result = new ArrayList<>();
    for (String id : ids) {
      result.add(new IdType(resourceType, id));
    }
    return result;
  }
}
