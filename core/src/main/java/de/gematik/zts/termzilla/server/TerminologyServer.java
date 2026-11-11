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
package de.gematik.zts.termzilla.server;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.StrictErrorHandler;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.tenant.UrlBaseTenantIdentificationStrategy;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.interceptor.BasicSecurityInterceptor;
import de.gematik.zts.termzilla.server.interceptor.HttpCachingInterceptor;
import de.gematik.zts.termzilla.server.interceptor.MyLogInterceptor;
import de.gematik.zts.termzilla.server.interceptor.OperationControlInterceptor;
import de.gematik.zts.termzilla.server.interceptor.OperationParameterValidationInterceptor;
import de.gematik.zts.termzilla.server.interceptor.RateLimitingInterceptor;
import de.gematik.zts.termzilla.server.interceptor.SearchParameterValidationInterceptor;
import de.gematik.zts.termzilla.server.provider.CodeSystemResourceProvider;
import de.gematik.zts.termzilla.server.provider.ConceptMapResourceProvider;
import de.gematik.zts.termzilla.server.provider.NativeBatchTransactionProvider;
import de.gematik.zts.termzilla.server.provider.TxCapabilityStatementProvider;
import de.gematik.zts.termzilla.server.provider.ValueSetResourceProvider;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import jakarta.servlet.annotation.WebServlet;
import org.springframework.web.cors.CorsConfiguration;

@WebServlet("/*")
public class TerminologyServer extends RestfulServer {

  private final transient CombiningTerminologyService terminologyService;
  private final transient ConfigProperties properties;

  public TerminologyServer(ConfigProperties properties) throws InvalidConfigurationException {
    properties.validate();
    this.properties = properties;
    this.terminologyService = new CombiningTerminologyService(properties);
  }

  @Override
  protected void initialize() {

    // initialize FHIR Context
    FhirContext ctx = FhirContext.forR4Cached();
    ctx.setParserErrorHandler(new StrictErrorHandler());
    setFhirContext(ctx);
    setServerName("ZTS Termzilla");

    // configure CORS
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    // headers
    properties.getCorsAllowedHeaders().forEach(corsConfiguration::addAllowedHeader);
    // origins
    properties.getCorsAllowedOrigins().forEach(corsConfiguration::addAllowedOrigin);
    // methods
    corsConfiguration.setAllowedMethods(properties.getCorsAllowedMethods());
    // credentials
    corsConfiguration.setAllowCredentials(properties.isCorsAllowCredentials());

    properties.getCorsExposedHeaders().forEach(corsConfiguration::addExposedHeader);

    CorsInterceptor corsInterceptor = new CorsInterceptor(corsConfiguration);
    registerInterceptor(corsInterceptor);

    // Operation control (check if operation is enabled)
    registerInterceptor(new OperationControlInterceptor(properties));

    // Caching-Interceptor registrieren
    registerInterceptor(
        new HttpCachingInterceptor(properties, terminologyService.getConfigAndDataStateHash()));
    // In HAPI eingebaute ETag-Unterstützung deaktivieren, da wir die ETags selbst über den
    // HttpCachingInterceptor berechnen und setzen.
    setETagSupport(ETagSupportEnum.DISABLED);

    // Search parameter validation (for search operations)
    registerInterceptor(new SearchParameterValidationInterceptor(properties));

    // Operation parameter validation (for custom operations)
    registerInterceptor(new OperationParameterValidationInterceptor(properties, ctx));

    // Basic security validation
    registerInterceptor(new BasicSecurityInterceptor(properties));

    // Rate limiting
    if (properties.isRateLimitEnabled()) {
      registerInterceptor(new RateLimitingInterceptor(properties.getRateLimitRequestsPerMinute()));
    }

    // Tenant-Identifikations-Strategie setzen. Dies benötigen wir, da wir mehrere "Sichten"
    // (Scopes)
    // auf denselben Datenbestand unterstützen wollen.
    setTenantIdentificationStrategy(new UrlBaseTenantIdentificationStrategy());

    // Registrieren der Resource Provider
    // Wir unterstützen lediglich CodeSystem und ValueSet Ressourcen
    registerProvider(new CodeSystemResourceProvider(terminologyService, properties));
    registerProvider(new ValueSetResourceProvider(terminologyService, properties));
    registerProvider(new ConceptMapResourceProvider(terminologyService, properties));
    if (properties.isSystemLevelBatchEnabled()) {
      registerProvider(new NativeBatchTransactionProvider(terminologyService, this, properties));
    }

    // Conformance Provider registrieren
    setServerConformanceProvider(
        new TxCapabilityStatementProvider(this, terminologyService, properties));

    // Logging-Interceptor
    registerInterceptor(new MyLogInterceptor());

    // Formatieren der zurückgegebenen Objekte
    // TODO do we want to use a ResponseHighlighterInterceptor in production or only for
    // testing/debugging?
  }
}
