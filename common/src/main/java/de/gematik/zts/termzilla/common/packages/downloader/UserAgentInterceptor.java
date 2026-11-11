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
package de.gematik.zts.termzilla.common.packages.downloader;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Adds a project-specific {@code User-Agent} header to outgoing HTTP requests. */
public class UserAgentInterceptor implements Interceptor {

  private static final String USERAGENT_PRODUCT = "termzilla-package-downloader";
  private static final String USERAGENT_COMMENT = "(zts-internal-tool)";

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request originalRequest = chain.request();
    Request requestWithUserAgent =
        originalRequest
            .newBuilder()
            .header(
                "User-Agent",
                USERAGENT_PRODUCT + "/" + getCommonVersion() + " " + USERAGENT_COMMENT)
            .build();
    return chain.proceed(requestWithUserAgent);
  }

  private static String getCommonVersion() {
    Package packageMetadata = UserAgentInterceptor.class.getPackage();
    if (packageMetadata != null && packageMetadata.getImplementationVersion() != null) {
      return packageMetadata.getImplementationVersion();
    }
    return "unknown";
  }
}
