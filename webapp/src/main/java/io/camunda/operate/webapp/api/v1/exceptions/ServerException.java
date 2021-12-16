/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.camunda.operate.webapp.api.v1.exceptions;

public class ServerException extends APIException{

  public static final String TYPE = "API application error";

  public ServerException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public ServerException(final String message) {
    super(message);
  }
}
