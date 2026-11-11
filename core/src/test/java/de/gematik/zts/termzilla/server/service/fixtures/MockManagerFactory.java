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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import de.gematik.zts.termzilla.api.service.CodeSystemSvcInterface;
import de.gematik.zts.termzilla.api.service.ConceptMapSvcInterface;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.api.service.ValueSetSvcInterface;
import java.util.List;
import java.util.Set;

/**
 * Factory for creating mock TerminologyManager instances for unit testing.
 *
 * <p>Provides convenient methods to create mock managers with specific configurations for testing
 * CombiningTerminologyService behavior.
 */
public class MockManagerFactory {

  /**
   * Creates a basic mock TerminologyManager with specified supported resources.
   *
   * @param managerName a descriptive name for the manager (for debugging)
   * @param supportedScopes set of scopes this manager supports
   * @param supportedCodeSystems list of CodeSystem canonical URLs
   * @param supportedValueSets list of ValueSet canonical URLs
   * @param supportedConceptMaps list of ConceptMap canonical URLs
   * @return a configured mock TerminologyManager
   */
  public static TerminologyManager createMockManager(
      String managerName,
      Set<String> supportedScopes,
      List<String> supportedCodeSystems,
      List<String> supportedValueSets,
      List<String> supportedConceptMaps) {

    TerminologyManager manager = mock(TerminologyManager.class, managerName);
    CodeSystemSvcInterface csSvc = mock(CodeSystemSvcInterface.class, managerName + ".cs()");
    ValueSetSvcInterface vsSvc = mock(ValueSetSvcInterface.class, managerName + ".vs()");
    ConceptMapSvcInterface cmSvc = mock(ConceptMapSvcInterface.class, managerName + ".cm()");

    // Setup manager
    lenient().when(manager.isInitialized()).thenReturn(true);
    lenient().when(manager.getTerminologyManagerName()).thenReturn(managerName);
    lenient().when(manager.getSupportedScopes()).thenReturn(supportedScopes);
    lenient().when(manager.cs()).thenReturn(csSvc);
    lenient().when(manager.vs()).thenReturn(vsSvc);
    lenient().when(manager.cm()).thenReturn(cmSvc);

    // Setup service interfaces
    lenient().when(csSvc.getSupportedCodeSystemCanonicalsList()).thenReturn(supportedCodeSystems);
    lenient().when(vsSvc.getSupportedValueSetCanonicalList()).thenReturn(supportedValueSets);
    lenient().when(cmSvc.getSupportedConceptMapCanonicalList()).thenReturn(supportedConceptMaps);

    return manager;
  }

  /**
   * Creates a mock manager with only CodeSystem support.
   *
   * @param managerName descriptive name
   * @param supportedCodeSystems list of supported CodeSystem URLs
   * @return configured mock manager
   */
  public static TerminologyManager createCodeSystemOnlyManager(
      String managerName, List<String> supportedCodeSystems) {
    return createMockManager(
        managerName, Set.of("fhir"), supportedCodeSystems, List.of(), List.of());
  }

  /**
   * Creates a mock manager with only ValueSet support.
   *
   * @param managerName descriptive name
   * @param supportedValueSets list of supported ValueSet URLs
   * @return configured mock manager
   */
  public static TerminologyManager createValueSetOnlyManager(
      String managerName, List<String> supportedValueSets) {
    return createMockManager(managerName, Set.of("fhir"), List.of(), supportedValueSets, List.of());
  }

  /**
   * Creates a mock manager that throws a specific exception for all operations.
   *
   * <p>Useful for testing error handling and exception aggregation behavior.
   *
   * @param managerName descriptive name
   * @param exceptionType the type of exception to throw
   * @param exceptionMessage the exception message
   * @return configured mock manager that throws exceptions
   */
  public static TerminologyManager createThrowingManager(
      String managerName, Class<? extends Exception> exceptionType, String exceptionMessage) {

    TerminologyManager manager = mock(TerminologyManager.class, managerName);
    CodeSystemSvcInterface csSvc = mock(CodeSystemSvcInterface.class, managerName + ".cs()");
    ValueSetSvcInterface vsSvc = mock(ValueSetSvcInterface.class, managerName + ".vs()");
    ConceptMapSvcInterface cmSvc = mock(ConceptMapSvcInterface.class, managerName + ".cm()");

    lenient().when(manager.isInitialized()).thenReturn(true);
    lenient().when(manager.getSupportedScopes()).thenReturn(Set.of("fhir"));
    lenient().when(manager.cs()).thenReturn(csSvc);
    lenient().when(manager.vs()).thenReturn(vsSvc);
    lenient().when(manager.cm()).thenReturn(cmSvc);

    // Setup to return empty lists (so manager gets registered)
    lenient().when(csSvc.getSupportedCodeSystemCanonicalsList()).thenReturn(List.of());
    lenient().when(vsSvc.getSupportedValueSetCanonicalList()).thenReturn(List.of());
    lenient().when(cmSvc.getSupportedConceptMapCanonicalList()).thenReturn(List.of());

    // Setup all methods to throw the specified exception
    try {
      Exception exceptionInstance =
          exceptionType.getConstructor(String.class).newInstance(exceptionMessage);

      lenient().when(csSvc.searchCodeSystem(anyString(), any())).thenThrow(exceptionInstance);
      lenient().when(vsSvc.searchValueSet(anyString(), any())).thenThrow(exceptionInstance);
      lenient().when(cmSvc.searchConceptMap(anyString(), any())).thenThrow(exceptionInstance);

    } catch (Exception e) {
      throw new RuntimeException("Failed to create throwing manager", e);
    }

    return manager;
  }

  /**
   * Creates a mock manager that is not initialized.
   *
   * <p>Useful for testing that CombiningTerminologyService correctly handles uninitialized
   * managers.
   *
   * @param managerName descriptive name
   * @return uninitialized mock manager
   */
  public static TerminologyManager createUninitializedManager(String managerName) {
    TerminologyManager manager = mock(TerminologyManager.class, managerName);
    lenient().when(manager.isInitialized()).thenReturn(false);
    return manager;
  }

  /**
   * Creates a mock manager with multiple scopes.
   *
   * @param managerName descriptive name
   * @param scopes set of supported scopes
   * @param supportedCodeSystems list of supported CodeSystem URLs
   * @return configured mock manager
   */
  public static TerminologyManager createMultiScopeManager(
      String managerName, Set<String> scopes, List<String> supportedCodeSystems) {
    return createMockManager(managerName, scopes, supportedCodeSystems, List.of(), List.of());
  }
}
