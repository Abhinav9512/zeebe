/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.camunda.operate.webapp.api.v1.exceptions;

public class ClientException extends APIException {

  public static final String TYPE = "Invalid request";

  public ClientException(final String message) {
    super(message);
  }

  public ClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
