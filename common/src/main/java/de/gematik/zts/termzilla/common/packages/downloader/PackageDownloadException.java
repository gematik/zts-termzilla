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

import java.io.Serial;
import lombok.Getter;

/** Exception thrown when package download operations fail. */
public class PackageDownloadException extends Exception {

  @Serial private static final long serialVersionUID = 1L;

  @Getter private final int statusCode;

  /**
   * Creates a new {@link PackageDownloadException} with a message.
   *
   * @param message the error message
   */
  public PackageDownloadException(String message) {
    super(message);
    this.statusCode = -1;
  }

  /**
   * Creates a new {@link PackageDownloadException} with a message and HTTP status code.
   *
   * @param message the error message
   * @param statusCode the HTTP status code
   */
  public PackageDownloadException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  /**
   * Creates a new {@link PackageDownloadException} with a message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public PackageDownloadException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = -1;
  }

  /**
   * Creates a new {@link PackageDownloadException} with a message, cause, and HTTP status code.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @param statusCode the HTTP status code
   */
  public PackageDownloadException(String message, Throwable cause, int statusCode) {
    super(message, cause);
    this.statusCode = statusCode;
  }
}
