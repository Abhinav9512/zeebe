/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.operate.webapp.api.v1.rest;

import io.camunda.operate.webapp.api.v1.entities.Error;
import io.camunda.operate.webapp.api.v1.exceptions.ClientException;
import io.camunda.operate.webapp.api.v1.exceptions.ResourceNotFoundException;
import io.camunda.operate.webapp.api.v1.exceptions.ServerException;
import io.camunda.operate.webapp.api.v1.exceptions.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

public abstract class ErrorController {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(ClientException.class)
  public ResponseEntity<Error> handleInvalidRequest(ClientException exception) {
    logger.info(exception.getMessage(), exception);
    final Error error = new Error()
        .setType(ClientException.TYPE)
        .setInstance(exception.getInstance())
        .setStatus(HttpStatus.BAD_REQUEST.value())
        .setMessage(exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(error);
  }

  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Error> handleAccessDeniedException(AccessDeniedException exception) {
    logger.info(exception.getMessage(), exception);
    final Error error = new Error()
        .setType(exception.getClass().getSimpleName())
        .setInstance(UUID.randomUUID().toString())
        .setStatus(HttpStatus.UNAUTHORIZED.value())
        .setMessage(exception.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(error);
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Error> handleException(Exception exception) {
    // Show client only detail message, log all messages
    return handleInvalidRequest(
        new ClientException(
            getOnlyDetailMessage(exception), exception)
    );
  }

  private String getOnlyDetailMessage(final Exception exception) {
    return StringUtils.substringBefore(exception.getMessage(), "; nested exception is");
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<Error> handleInvalidRequest(ValidationException exception) {
    logger.info(exception.getMessage(), exception);
    final Error error = new Error()
        .setType(ValidationException.TYPE)
        .setInstance(exception.getInstance())
        .setStatus(HttpStatus.BAD_REQUEST.value())
        .setMessage(exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(error);
  }



  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Error> handleNotFound(ResourceNotFoundException exception) {
    logger.info(exception.getMessage(), exception);
    final Error error = new Error()
        .setType(ResourceNotFoundException.TYPE)
        .setInstance(exception.getInstance())
        .setStatus(HttpStatus.NOT_FOUND.value())
        .setMessage(exception.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(error);
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(ServerException.class)
  public ResponseEntity<Error> handleServerException(ServerException exception) {
    logger.error(exception.getMessage(), exception);
    final Error error = new Error()
        .setType(ServerException.TYPE)
        .setInstance(exception.getInstance())
        .setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .setMessage(exception.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(error);
  }
}