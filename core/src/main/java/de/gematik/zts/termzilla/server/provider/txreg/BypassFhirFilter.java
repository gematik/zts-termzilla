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
package de.gematik.zts.termzilla.server.provider.txreg;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

public class BypassFhirFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String path = httpRequest.getRequestURI();

    if (path.startsWith("/termzilla-reg/resolve")) {
      // Explizit aus dem Filter-Chain "aussteigen" und Spring MVC übernehmen lassen
      // Das HAPI Servlet wird dann nicht mehr aufgerufen

      // WICHTIG: hier darf NICHT an getRequestDispatcher weitergeleitet werden
      // Spring MVC DispatcherServlet übernimmt automatisch, wenn chain.doFilter übersprungen wird

      // request.setAttribute("SPRING_MVC_BYPASS", true); // optional für Debug
      request
          .getServletContext()
          .getNamedDispatcher("dispatcherServlet")
          .forward(request, response);
      return;
    }

    // Bypass for actuator endpoints (health probes)
    if (path.startsWith("/actuator")) {
      request
          .getServletContext()
          .getNamedDispatcher("dispatcherServlet")
          .forward(request, response);
      return;
    }

    // Normale Weitergabe
    chain.doFilter(request, response);
  }
}
