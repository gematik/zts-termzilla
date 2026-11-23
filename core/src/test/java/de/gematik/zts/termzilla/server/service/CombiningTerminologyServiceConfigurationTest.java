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
package de.gematik.zts.termzilla.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.api.service.CodeSystemSvcInterface;
import de.gematik.zts.termzilla.api.service.ConceptMapSvcInterface;
import de.gematik.zts.termzilla.api.service.Terminology;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.api.service.TerminologyProvider;
import de.gematik.zts.termzilla.api.service.ValueSetSvcInterface;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CombiningTerminologyServiceConfigurationTest {

  @ParameterizedTest
  @ValueSource(strings = {"fhir", "custom-scope"})
  void passesConfiguredScopeToProviders(String scope) throws Exception {
    ConfigProperties properties = new ConfigProperties();
    properties.setBaseWorkingDir("/tmp/termzilla-test");
    properties.setSystemLanguage("de-DE");
    properties.setDefaultScope(scope);
    TerminologyProvider provider = mock(TerminologyProvider.class);
    TerminologyManager manager = mock(TerminologyManager.class);
    when(manager.isInitialized()).thenReturn(true);
    when(manager.getSupportedScopes()).thenReturn(Set.of(scope));
    when(manager.getTerminologyManagerName()).thenReturn("test");
    when(manager.getConfigurationAndDataStateHash()).thenReturn("test");
    when(manager.cs()).thenReturn(mock(CodeSystemSvcInterface.class));
    when(manager.vs()).thenReturn(mock(ValueSetSvcInterface.class));
    when(manager.cm()).thenReturn(mock(ConceptMapSvcInterface.class));
    Path baseDir = Path.of(properties.getBaseWorkingDir());
    when(provider.create(baseDir, scope)).thenReturn(manager);

    try (var terminology = mockStatic(Terminology.class)) {
      terminology.when(Terminology::providers).thenReturn(List.of(provider));
      var service = new CombiningTerminologyService(properties);
      assertThat(service.getSupportedScopes()).containsExactly(scope);
      service.checkScope(scope);
    }

    verify(provider).create(baseDir, scope);
  }
}
