/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.job;

import io.camunda.optimize.dto.optimize.ImportRequestDto;
import io.camunda.optimize.dto.optimize.importing.FlowNodeEventDto;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.writer.activity.CompletedActivityInstanceWriter;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.ArrayList;
import java.util.List;

public class CompletedActivityInstanceDatabaseImportJob
    extends DatabaseImportJob<FlowNodeEventDto> {

  private final CompletedActivityInstanceWriter completedActivityInstanceWriter;
  private final ConfigurationService configurationService;

  public CompletedActivityInstanceDatabaseImportJob(
      final CompletedActivityInstanceWriter completedActivityInstanceWriter,
      final ConfigurationService configurationService,
      final Runnable callback,
      final DatabaseClient databaseClient) {
    super(callback, databaseClient);
    this.completedActivityInstanceWriter = completedActivityInstanceWriter;
    this.configurationService = configurationService;
  }

  @Override
  protected void persistEntities(final List<FlowNodeEventDto> newOptimizeEntities) {
    final List<ImportRequestDto> importRequests = new ArrayList<>();
    importRequests.addAll(
        completedActivityInstanceWriter.generateActivityInstanceImports(newOptimizeEntities));
    databaseClient.executeImportRequestsAsBulk(
        "Completed activity instances",
        importRequests,
        configurationService.getSkipDataAfterNestedDocLimitReached());
  }
}
