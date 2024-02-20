/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.gateway.rest;

import io.camunda.zeebe.auth.api.JwtAuthorizationBuilder;
import io.camunda.zeebe.auth.impl.Authorization;
import io.camunda.zeebe.gateway.protocol.rest.UserTaskCompletionRequest;
import io.camunda.zeebe.gateway.rest.impl.broker.request.BrokerUserTaskCompletionRequest;
import io.camunda.zeebe.msgpack.value.DocumentValue;
import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter;
import io.camunda.zeebe.protocol.record.value.TenantOwned;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.springframework.web.server.ServerWebExchange;

public class RequestMapper {

  // TODO: create proper multi-tenancy handling, e.g. via HTTP filter
  public static final String TENANT_CTX_KEY = "io.camunda.zeebe.broker.rest.tenantIds";

  public static BrokerUserTaskCompletionRequest toUserTaskCompletionRequest(
      final UserTaskCompletionRequest completionRequest,
      final long userTaskKey,
      final ServerWebExchange context) {

    final var brokerRequest =
        new BrokerUserTaskCompletionRequest(
            userTaskKey,
            getDocumentOrEmpty(completionRequest, UserTaskCompletionRequest::getVariables),
            getStringOrEmpty(completionRequest, UserTaskCompletionRequest::getAction));

    final List<String> authorizedTenants =
        context.getAttributeOrDefault(
            TENANT_CTX_KEY, List.of(TenantOwned.DEFAULT_TENANT_IDENTIFIER));

    final String authorizationToken =
        Authorization.jwtEncoder()
            .withIssuer(JwtAuthorizationBuilder.DEFAULT_ISSUER)
            .withAudience(JwtAuthorizationBuilder.DEFAULT_AUDIENCE)
            .withSubject(JwtAuthorizationBuilder.DEFAULT_SUBJECT)
            .withClaim(Authorization.AUTHORIZED_TENANTS, authorizedTenants)
            .encode();
    brokerRequest.setAuthorization(authorizationToken);

    return brokerRequest;
  }

  private static DirectBuffer getDocumentOrEmpty(
      final UserTaskCompletionRequest request,
      final Function<UserTaskCompletionRequest, Map<String, Object>> variablesExtractor) {
    final Map<String, Object> value = request == null ? null : variablesExtractor.apply(request);
    return value == null || value.isEmpty()
        ? DocumentValue.EMPTY_DOCUMENT
        : new UnsafeBuffer(MsgPackConverter.convertToMsgPack(value));
  }

  private static String getStringOrEmpty(
      final UserTaskCompletionRequest request,
      final Function<UserTaskCompletionRequest, String> valueExtractor) {
    final String value = request == null ? null : valueExtractor.apply(request);
    return value == null ? "" : value;
  }
}