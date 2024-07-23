/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.service;

import io.camunda.optimize.dto.engine.HistoricActivityInstanceEngineDto;
import io.camunda.optimize.dto.optimize.importing.FlowNodeEventDto;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.writer.activity.RunningActivityInstanceWriter;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.importing.DatabaseImportJobExecutor;
import io.camunda.optimize.service.importing.engine.service.definition.ProcessDefinitionResolverService;
import io.camunda.optimize.service.importing.job.RunningActivityInstanceDatabaseImportJob;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunningActivityInstanceImportService
    implements ImportService<HistoricActivityInstanceEngineDto> {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  protected DatabaseImportJobExecutor databaseImportJobExecutor;
  protected EngineContext engineContext;
  private final RunningActivityInstanceWriter runningActivityInstanceWriter;
  private final ProcessDefinitionResolverService processDefinitionResolverService;
  private final ConfigurationService configurationService;
  private final DatabaseClient databaseClient;

  public RunningActivityInstanceImportService(
      final RunningActivityInstanceWriter runningActivityInstanceWriter,
      final EngineContext engineContext,
      final ConfigurationService configurationService,
      final ProcessDefinitionResolverService processDefinitionResolverService,
      final DatabaseClient databaseClient) {
    databaseImportJobExecutor =
        new DatabaseImportJobExecutor(getClass().getSimpleName(), configurationService);
    this.engineContext = engineContext;
    this.runningActivityInstanceWriter = runningActivityInstanceWriter;
    this.processDefinitionResolverService = processDefinitionResolverService;
    this.configurationService = configurationService;
    this.databaseClient = databaseClient;
  }

  @Override
  public void executeImport(
      final List<HistoricActivityInstanceEngineDto> pageOfEngineEntities,
      final Runnable importCompleteCallback) {
    logger.trace("Importing running activity instances from engine...");

    final boolean newDataIsAvailable = !pageOfEngineEntities.isEmpty();
    if (newDataIsAvailable) {
      final List<FlowNodeEventDto> newOptimizeEntities =
          mapEngineEntitiesToOptimizeEntities(pageOfEngineEntities);
      final DatabaseImportJob<FlowNodeEventDto> databaseImportJob =
          createDatabaseImportJob(newOptimizeEntities, importCompleteCallback);
      addDatabaseImportJobToQueue(databaseImportJob);
    }
  }

  @Override
  public DatabaseImportJobExecutor getDatabaseImportJobExecutor() {
    return databaseImportJobExecutor;
  }

  private void addDatabaseImportJobToQueue(final DatabaseImportJob databaseImportJob) {
    databaseImportJobExecutor.executeImportJob(databaseImportJob);
  }

  private List<FlowNodeEventDto> mapEngineEntitiesToOptimizeEntities(
      final List<HistoricActivityInstanceEngineDto> engineEntities) {
    return engineEntities.stream()
        .map(
            activity ->
                processDefinitionResolverService.enrichEngineDtoWithDefinitionKey(
                    engineContext,
                    activity,
                    HistoricActivityInstanceEngineDto::getProcessDefinitionKey,
                    HistoricActivityInstanceEngineDto::getProcessDefinitionId,
                    HistoricActivityInstanceEngineDto::setProcessDefinitionKey))
        .filter(activity -> activity.getProcessDefinitionKey() != null)
        .map(this::mapEngineEntityToOptimizeEntity)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private DatabaseImportJob<FlowNodeEventDto> createDatabaseImportJob(
      final List<FlowNodeEventDto> events, final Runnable callback) {
    final RunningActivityInstanceDatabaseImportJob activityImportJob =
        new RunningActivityInstanceDatabaseImportJob(
            runningActivityInstanceWriter, configurationService, callback, databaseClient);
    activityImportJob.setEntitiesToImport(events);
    return activityImportJob;
  }

  private Optional<FlowNodeEventDto> mapEngineEntityToOptimizeEntity(
      final HistoricActivityInstanceEngineDto engineEntity) {
    return processDefinitionResolverService
        .getDefinition(engineEntity.getProcessDefinitionId(), engineContext)
        .map(
            definition ->
                new FlowNodeEventDto(
                    engineEntity.getId(),
                    engineEntity.getActivityId(),
                    engineEntity.getActivityType(),
                    engineEntity.getActivityName(),
                    engineEntity.getStartTime(),
                    definition.getId(),
                    definition.getKey(),
                    definition.getVersion(),
                    engineEntity
                        .getTenantId()
                        .orElseGet(() -> engineContext.getDefaultTenantId().orElse(null)),
                    engineContext.getEngineAlias(),
                    engineEntity.getProcessInstanceId(),
                    engineEntity.getStartTime(),
                    null,
                    null,
                    engineEntity.getSequenceCounter(),
                    engineEntity.getCanceled(),
                    engineEntity.getTaskId()));
  }
}
