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
package de.gematik.zts.termzilla.common.codesystem.search;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.param.UriParamQualifierEnum;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;

/** Reusable in-memory filtering logic for CodeSystem search operations. */
public final class InMemoryCodeSystemSearchHelper {

  private InMemoryCodeSystemSearchHelper() {}

  public static List<IdType> search(
      List<CodeSystem> codeSystems,
      CsSearchParameter csSearchParameter,
      Consumer<CodeSystem> resultObserver)
      throws InvalidRequestException, NotImplementedException {

    validateNotImplemented(csSearchParameter);

    List<CodeSystem> filteredList = new ArrayList<>(codeSystems);

    if (csSearchParameter.isContentSearch()) {
      filteredList = applyContentSearch(filteredList, csSearchParameter);
    }

    if (csSearchParameter.isMetadataSearch()) {
      filteredList = applyMetadataSearch(filteredList, csSearchParameter);
    }

    return buildResult(filteredList, resultObserver);
  }

  private static void validateNotImplemented(CsSearchParameter csSearchParameter)
      throws NotImplementedException {
    if (csSearchParameter.isCodeSearch()) {
      throw new NotImplementedException("Code search not implemented");
    }
    if (csSearchParameter.getContentMode().isPresent()) {
      throw new NotImplementedException("Search by Content-Mode not implemented");
    }
    if (csSearchParameter.getStatus().isPresent()) {
      throw new NotImplementedException("Search by Status not implemented");
    }
  }

  private static List<CodeSystem> applyContentSearch(
      List<CodeSystem> codeSystems, CsSearchParameter csSearchParameter)
      throws NotImplementedException {

    var content = csSearchParameter.getContent();
    if (content.isEmpty()) {
      return new ArrayList<>();
    }

    String value = content.get().getValue();
    Set<CodeSystem> results = new LinkedHashSet<>();

    results.addAll(filterByUrl(codeSystems, new UriParam(value)));
    results.addAll(filterByVersion(codeSystems, new TokenParam(value)));
    results.addAll(filterByName(codeSystems, new StringParam(value)));
    results.addAll(filterByDescription(codeSystems, new StringParam(value)));
    results.addAll(filterByPublisher(codeSystems, new StringParam(value)));

    return new ArrayList<>(results);
  }

  private static List<CodeSystem> applyMetadataSearch(
      List<CodeSystem> codeSystems, CsSearchParameter csSearchParameter)
      throws NotImplementedException {

    List<CodeSystem> filtered = codeSystems;

    var systemParam = csSearchParameter.getSystem();
    if (systemParam.isPresent()) {
      filtered = filterByUrl(filtered, systemParam.get());
    }

    var urlParam = csSearchParameter.getUrl();
    if (urlParam.isPresent()) {
      filtered = filterByUrl(filtered, urlParam.get());
    }

    var versionParam = csSearchParameter.getVersion();
    if (versionParam.isPresent()) {
      filtered = filterByVersion(filtered, versionParam.get());
    }

    var nameParam = csSearchParameter.getName();
    if (nameParam.isPresent()) {
      filtered = filterByName(filtered, nameParam.get());
    }

    var titleParam = csSearchParameter.getTitle();
    if (titleParam.isPresent()) {
      filtered = filterByTitle(filtered, titleParam.get());
    }

    var descriptionParam = csSearchParameter.getDescription();
    if (descriptionParam.isPresent()) {
      filtered = filterByDescription(filtered, descriptionParam.get());
    }

    var publisherParam = csSearchParameter.getPublisher();
    if (publisherParam.isPresent()) {
      filtered = filterByPublisher(filtered, publisherParam.get());
    }

    // Source filter - if present and not empty, return empty list
    var sourceParam = csSearchParameter.getSource();
    if (sourceParam.isPresent()) {
      String sourceValue = sourceParam.get().getValue();
      if (StringUtils.isNotEmpty(sourceValue)) {
        return new ArrayList<>();
      }
    }

    return filtered;
  }

  private static List<IdType> buildResult(
      List<CodeSystem> codeSystems, Consumer<CodeSystem> resultObserver) {

    if (resultObserver != null) {
      codeSystems.forEach(resultObserver);
    }

    return codeSystems.stream().map(cs -> new IdType(cs.getId())).toList();
  }

  public static List<CodeSystem> filterByUrl(List<CodeSystem> list, UriParam urlParam)
      throws NotImplementedException {

    // Validate qualifiers upfront
    if (urlParam.getQualifier() == UriParamQualifierEnum.ABOVE) {
      throw new NotImplementedException("Qualifier ABOVE not implemented");
    }
    if (urlParam.getQualifier() == UriParamQualifierEnum.BELOW) {
      throw new NotImplementedException("Qualifier BELOW not implemented");
    }

    return list.stream()
        .filter(
            cs -> {
              if (urlParam.getMissing() != null) {
                return (Boolean.TRUE.equals(urlParam.getMissing()) && !cs.hasUrl())
                    || (Boolean.FALSE.equals(urlParam.getMissing()) && cs.hasUrl());
              }
              return Strings.CS.equals(cs.getUrl(), urlParam.getValue());
            })
        .toList();
  }

  public static List<CodeSystem> filterByVersion(List<CodeSystem> list, TokenParam versionParam) {
    return list.stream()
        .filter(
            cs -> {
              if (versionParam.getMissing() != null) {
                return (Boolean.TRUE.equals(versionParam.getMissing()) && !cs.hasVersion())
                    || (Boolean.FALSE.equals(versionParam.getMissing()) && cs.hasVersion());
              }
              return Strings.CS.equals(cs.getVersion(), versionParam.getValue());
            })
        .toList();
  }

  public static List<CodeSystem> filterByName(List<CodeSystem> list, StringParam nameParam) {
    List<CodeSystem> filteredList = new ArrayList<>();
    for (CodeSystem cs : list) {
      if (addBasedOnStringParam(cs.hasName(), cs.getName(), nameParam)) {
        filteredList.add(cs);
      }
    }
    return filteredList;
  }

  public static List<CodeSystem> filterByTitle(List<CodeSystem> list, StringParam titleParam) {
    List<CodeSystem> filteredList = new ArrayList<>();
    for (CodeSystem cs : list) {
      if (addBasedOnStringParam(cs.hasTitle(), cs.getTitle(), titleParam)) {
        filteredList.add(cs);
      }
    }
    return filteredList;
  }

  public static List<CodeSystem> filterByDescription(
      List<CodeSystem> list, StringParam descriptionParam) {

    List<CodeSystem> filteredList = new ArrayList<>();
    for (CodeSystem cs : list) {
      if (addBasedOnStringParam(cs.hasDescription(), cs.getDescription(), descriptionParam)) {
        filteredList.add(cs);
      }
    }
    return filteredList;
  }

  public static List<CodeSystem> filterByPublisher(
      List<CodeSystem> list, StringParam publisherParam) {
    List<CodeSystem> filteredList = new ArrayList<>();
    for (CodeSystem cs : list) {
      if (addBasedOnStringParam(cs.hasPublisher(), cs.getPublisher(), publisherParam)) {
        filteredList.add(cs);
      }
    }
    return filteredList;
  }

  private static boolean addBasedOnStringParam(
      boolean hasElement, String elementValue, StringParam param) {
    if (param.getMissing() != null) {
      return (Boolean.TRUE.equals(param.getMissing()) && !hasElement)
          || (Boolean.FALSE.equals(param.getMissing()) && hasElement);
    }

    if (param.isExact()) {
      return Strings.CS.equals(elementValue, param.getValue());
    }

    if (param.isContains()) {
      return Strings.CI.contains(elementValue, param.getValue());
    }

    return Strings.CS.startsWith(elementValue, param.getValue());
  }
}
