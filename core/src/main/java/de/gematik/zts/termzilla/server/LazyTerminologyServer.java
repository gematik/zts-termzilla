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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.health.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serial;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyTerminologyServer extends HttpServlet {

  @Serial private static final long serialVersionUID = 1L;

  private final transient ConfigProperties properties;
  private final AtomicReference<TerminologyServer> delegate = new AtomicReference<>();
  private final transient ObjectMapper objectMapper = new ObjectMapper();
  private final transient ExecutorService initExecutor;

  public LazyTerminologyServer(ConfigProperties properties) {
    this.properties = properties;
    // Create dedicated thread for initialization
    this.initExecutor =
        Executors.newSingleThreadExecutor(
            r -> {
              Thread t = new Thread(r, "terminology-init");
              t.setDaemon(false); // Must not be daemon
              t.setUncaughtExceptionHandler(
                  (thread, throwable) ->
                      log.error("Uncaught exception in initialization thread", throwable));
              return t;
            });
  }

  @Override
  public void init() throws ServletException {
    super.init();

    log.info("=== Submitting initialization task ===");
    log.info("Available processors: {}", Runtime.getRuntime().availableProcessors());
    log.info(
        "ForkJoinPool commonPool parallelism: {}",
        java.util.concurrent.ForkJoinPool.commonPool().getParallelism());

    // Use explicit executor instead of ForkJoinPool.commonPool()
    initExecutor.submit(
        () -> {
          try {
            log.info("=== Initialization thread STARTED ===");
            log.info("Thread: {}", Thread.currentThread().getName());

            TerminologyServer server = new TerminologyServer(properties);
            server.init(getServletConfig());
            delegate.set(server);

            log.info("=== Initialization thread COMPLETED ===");
          } catch (Throwable e) {
            log.error("=== Initialization thread FAILED ===", e);
          }
        });

    log.info("=== Initialization task submitted ===");
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    TerminologyServer server = delegate.get();
    if (server == null) {
      sendServiceUnavailableResponse(req, resp);
      return;
    }

    server.service(req, resp);
  }

  public boolean isInitialized() {
    return delegate.get() != null;
  }

  @Override
  public void destroy() {
    log.info("Shutting down initialization executor");
    initExecutor.shutdown();
    try {
      if (!initExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
        initExecutor.shutdownNow();
      }
    } catch (InterruptedException _) {
      initExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
    super.destroy();
  }

  private void sendServiceUnavailableResponse(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    resp.setStatus(503);
    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .detail("Service initializing, please wait...")
            .instance(req.getRequestURI())
            .status(503)
            .title("Der Service ist derzeit nicht verfügbar (SERVICE_UNAVAILABLE)")
            .timestamp(System.currentTimeMillis())
            .build();

    objectMapper.writeValue(resp.getWriter(), errorResponse);
  }
}
