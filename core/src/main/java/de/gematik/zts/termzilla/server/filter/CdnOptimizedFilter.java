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
package de.gematik.zts.termzilla.server.filter;

import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ACCEPT;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ACCEPT_LANGUAGE;

import de.gematik.zts.termzilla.configuration.ConfigProperties;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Servlet filter that strips {@code Accept} and {@code Accept-Language} headers from incoming
 * requests when CDN-optimized mode is enabled.
 *
 * <p>When active, downstream components (including the HAPI FHIR servlet) no longer see the
 * client-supplied content-negotiation headers. As a result, the server always responds with its
 * configured defaults for format and language, and the {@code Vary: Accept, Accept-Language}
 * response header can be omitted, improving CDN cache efficiency.
 *
 * <p>This filter runs before the HAPI FHIR servlet because Spring Boot servlet filters are executed
 * prior to servlet dispatch.
 */
@Slf4j
@Component
public class CdnOptimizedFilter implements Filter {

  private static final Set<String> STRIPPED_HEADERS =
      Set.of(ACCEPT.toLowerCase(Locale.ROOT), ACCEPT_LANGUAGE.toLowerCase(Locale.ROOT));

  private final boolean enabled;

  /**
   * Creates a new {@code CdnOptimizedFilter} using the given configuration.
   *
   * @param config the server configuration; CDN-optimized mode is active when {@link
   *     ConfigProperties#isCachingCdnOptimized()} returns {@code true}
   */
  public CdnOptimizedFilter(ConfigProperties config) {
    this.enabled = config.isCachingCdnOptimized();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (enabled && request instanceof HttpServletRequest httpRequest) {
      log.debug("CDN-optimized mode: stripping Accept and Accept-Language headers");
      chain.doFilter(new HeaderStrippingRequestWrapper(httpRequest), response);
    } else {
      chain.doFilter(request, response);
    }
  }

  /**
   * {@link HttpServletRequestWrapper} that hides the configured headers from downstream components.
   */
  private static class HeaderStrippingRequestWrapper extends HttpServletRequestWrapper {

    HeaderStrippingRequestWrapper(HttpServletRequest request) {
      super(request);
    }

    @Override
    public String getHeader(String name) {
      return STRIPPED_HEADERS.contains(name.toLowerCase(Locale.ROOT))
          ? null
          : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      return STRIPPED_HEADERS.contains(name.toLowerCase(Locale.ROOT))
          ? Collections.emptyEnumeration()
          : super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      List<String> names =
          Collections.list(super.getHeaderNames()).stream()
              .filter(n -> !STRIPPED_HEADERS.contains(n.toLowerCase(Locale.ROOT)))
              .toList();
      return Collections.enumeration(names);
    }
  }
}
