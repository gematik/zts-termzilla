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
package de.gematik.zts.termzilla.server.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor
public class MyLogInterceptor {

  private static final Logger ourLog = LoggerFactory.getLogger(MyLogInterceptor.class);
  private String myErrorMessageFormat = "ERROR - ${operationType} - ${idOrResourceName}";
  private boolean myLogExceptions = true;
  private Logger myLogger;
  private String myMessageFormat;

  public MyLogInterceptor() {
    this.myLogger = ourLog;
    this.myMessageFormat = "${operationType} - ${idOrResourceName}";
  }

  public String getMessageFormat() {
    return this.myMessageFormat;
  }

  public String getErrorMessageFormat() {
    return this.myErrorMessageFormat;
  }

  @Hook(Pointcut.SERVER_HANDLE_EXCEPTION)
  public boolean handleException(
      RequestDetails theRequestDetails,
      BaseServerResponseException theException,
      HttpServletRequest theServletRequest,
      HttpServletResponse theServletResponse) {
    if (this.myLogExceptions) {

      this.myLogger.info(
          "{} -> ERROR: {}", theRequestDetails.getCompleteUrl(), theException.getMessage());
    }

    return true;
  }

  @Hook(Pointcut.SERVER_PROCESSING_COMPLETED_NORMALLY)
  public void processingCompletedNormally(ServletRequestDetails theRequestDetails) {

    this.myLogger.info("{} -> OK", theRequestDetails.getCompleteUrl());
  }

  public boolean isLogExceptions() {
    return this.myLogExceptions;
  }

  public void setErrorMessageFormat(String theErrorMessageFormat) {
    Validate.notBlank(theErrorMessageFormat, "Message format can not be null/empty");
    this.myErrorMessageFormat = theErrorMessageFormat;
  }

  public void setLogExceptions(boolean theLogExceptions) {
    this.myLogExceptions = theLogExceptions;
  }

  public void setLogger(Logger theLogger) {
    Validate.notNull(theLogger, "Logger can not be null");
    this.myLogger = theLogger;
  }
}
