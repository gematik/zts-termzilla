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
package de.gematik.zts.termzilla;

import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.LazyTerminologyServer;
import de.gematik.zts.termzilla.server.provider.txreg.BypassFhirFilter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Servlet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication(scanBasePackages = {"de.gematik.zts.termzilla"})
public class Termzilla {

  public static void main(String[] args) {
    SpringApplication.run(Termzilla.class, args);
  }

  @Bean
  public LazyTerminologyServer lazyTerminologyServer(ConfigProperties configProperties) {
    return new LazyTerminologyServer(configProperties);
  }

  @Bean
  public ServletRegistrationBean<Servlet> fhirServletRegistrationBean(
      LazyTerminologyServer lazyTerminologyServer) {
    ServletRegistrationBean<Servlet> registration =
        new ServletRegistrationBean<>(lazyTerminologyServer, "/*");
    registration.setName("FhirServlet");
    registration.setLoadOnStartup(1);

    return registration;
  }

  @Bean
  public FilterRegistrationBean<BypassFhirFilter> bypassFhirFilterRegistration() {
    FilterRegistrationBean<BypassFhirFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new BypassFhirFilter());
    registrationBean.setOrder(0); // Früh ausführen
    registrationBean.addUrlPatterns("/*"); // Auf alles anwenden
    return registrationBean;
  }

  @PostConstruct
  public void logEnvironment() {
    log.info("=== Environment Variables ===");
    System.getenv()
        .forEach(
            (key, value) -> {
              // Optional: Filter sensitive Informationen
              if (key.contains("FHIRTERMS") || key.contains("SNOMED") || key.contains("LOINC")) {
                log.info("{} = {}", key, value);
              }
            });
  }
}
