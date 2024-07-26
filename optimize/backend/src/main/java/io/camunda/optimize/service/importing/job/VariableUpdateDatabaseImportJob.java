/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.job;

import io.camunda.optimize.dto.optimize.ImportRequestDto;
import io.camunda.optimize.dto.optimize.query.variable.ProcessVariableDto;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.writer.variable.ProcessVariableUpdateWriter;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.ArrayList;
import java.util.List;

public class VariableUpdateDatabaseImportJob extends DatabaseImportJob<ProcessVariableDto> {

  private final ProcessVariableUpdateWriter processVariableUpdateWriter;
  private final ConfigurationService configurationService;

  public VariableUpdateDatabaseImportJob(
      final ProcessVariableUpdateWriter variableWriter,
      final ConfigurationService configurationService,
      final Runnable callback,
      final DatabaseClient databaseClient) {
    super(callback, databaseClient);
    processVariableUpdateWriter = variableWriter;
    this.configurationService = configurationService;
  }

  @Override
  protected void persistEntities(final List<ProcessVariableDto> variableUpdates) {
    final List<ImportRequestDto> importBulks = new ArrayList<>();
    importBulks.addAll(processVariableUpdateWriter.generateVariableUpdateImports(variableUpdates));
    databaseClient.executeImportRequestsAsBulk(
        "Variable updates",
        importBulks,
        configurationService.getSkipDataAfterNestedDocLimitReached());
  }
}
