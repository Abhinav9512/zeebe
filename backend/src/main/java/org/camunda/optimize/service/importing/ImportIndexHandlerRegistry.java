/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.importing;

import lombok.RequiredArgsConstructor;
import org.camunda.optimize.service.importing.engine.handler.CompletedActivityInstanceImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.CompletedIncidentImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.CompletedProcessInstanceImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.CompletedUserTaskInstanceImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.DecisionDefinitionImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.DecisionDefinitionXmlImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.DecisionInstanceImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.EngineImportIndexHandlerProvider;
import org.camunda.optimize.service.importing.engine.handler.IdentityLinkLogImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.OpenIncidentImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.ProcessDefinitionImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.ProcessDefinitionXmlImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.RunningActivityInstanceImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.RunningProcessInstanceImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.RunningUserTaskInstanceImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.TenantImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.UserOperationLogImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.VariableUpdateInstanceImportIndexHandler;
import org.camunda.optimize.service.importing.zeebe.handler.ZeebeImportIndexHandlerProvider;
import org.camunda.optimize.service.importing.zeebe.handler.ZeebeProcessDefinitionImportIndexHandler;
import org.camunda.optimize.service.importing.zeebe.handler.ZeebeProcessInstanceImportIndexHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ImportIndexHandlerRegistry {

  private Map<String, EngineImportIndexHandlerProvider> engineImportIndexHandlerProviderMap = new HashMap<>();
  private Map<Integer, ZeebeImportIndexHandlerProvider> zeebeImportIndexHandlerProviderMap = new HashMap<>();

  public void register(final String engineAlias,
                       final EngineImportIndexHandlerProvider engineImportIndexHandlerProvider) {
    engineImportIndexHandlerProviderMap.put(engineAlias, engineImportIndexHandlerProvider);
  }

  public void register(final int partitionId,
                       final ZeebeImportIndexHandlerProvider zeebeImportIndexHandlerProvider) {
    zeebeImportIndexHandlerProviderMap.put(partitionId, zeebeImportIndexHandlerProvider);
  }

  public List<AllEntitiesBasedImportIndexHandler> getAllEntitiesBasedHandlers(String engineAlias) {
    List<AllEntitiesBasedImportIndexHandler> result = new ArrayList<>();
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider =
      engineImportIndexHandlerProviderMap.get(engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getAllEntitiesBasedHandlers();
    }
    return result;
  }

  public List<TimestampBasedEngineImportIndexHandler> getTimestampEngineBasedHandlers(String engineAlias) {
    List<TimestampBasedEngineImportIndexHandler> result = new ArrayList<>();
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider =
      engineImportIndexHandlerProviderMap.get(engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getTimestampBasedEngineHandlers();
    }
    return result;
  }

  public CompletedProcessInstanceImportIndexHandler getCompletedProcessInstanceImportIndexHandler(String engineAlias) {
    CompletedProcessInstanceImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(CompletedProcessInstanceImportIndexHandler.class);
    }
    return result;
  }

  public CompletedActivityInstanceImportIndexHandler getCompletedActivityInstanceImportIndexHandler(String engineAlias) {
    CompletedActivityInstanceImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result =
        engineImportIndexHandlerProvider.getImportIndexHandler(CompletedActivityInstanceImportIndexHandler.class);
    }
    return result;
  }

  public RunningActivityInstanceImportIndexHandler getRunningActivityInstanceImportIndexHandler(String engineAlias) {
    RunningActivityInstanceImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(RunningActivityInstanceImportIndexHandler.class);
    }
    return result;
  }

  public CompletedIncidentImportIndexHandler getCompletedIncidentImportIndexHandler(String engineAlias) {
    CompletedIncidentImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider =
      engineImportIndexHandlerProviderMap.get(engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(CompletedIncidentImportIndexHandler.class);
    }
    return result;
  }

  public OpenIncidentImportIndexHandler getOpenIncidentImportIndexHandler(String engineAlias) {
    OpenIncidentImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(OpenIncidentImportIndexHandler.class);
    }
    return result;
  }

  public UserOperationLogImportIndexHandler getUserOperationsLogImportIndexHandler(String engineAlias) {
    UserOperationLogImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(UserOperationLogImportIndexHandler.class);
    }
    return result;
  }

  public RunningProcessInstanceImportIndexHandler getRunningProcessInstanceImportIndexHandler(String engineAlias) {
    RunningProcessInstanceImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(RunningProcessInstanceImportIndexHandler.class);
    }
    return result;
  }

  public VariableUpdateInstanceImportIndexHandler getRunningVariableInstanceImportIndexHandler(String engineAlias) {
    VariableUpdateInstanceImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(VariableUpdateInstanceImportIndexHandler.class);
    }
    return result;
  }

  public ProcessDefinitionImportIndexHandler getProcessDefinitionImportIndexHandler(String engineAlias) {
    ProcessDefinitionImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(ProcessDefinitionImportIndexHandler.class);
    }
    return result;
  }

  public CompletedUserTaskInstanceImportIndexHandler getCompletedUserTaskInstanceImportIndexHandler(String engineAlias) {
    CompletedUserTaskInstanceImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result =
        engineImportIndexHandlerProvider.getImportIndexHandler(CompletedUserTaskInstanceImportIndexHandler.class);
    }
    return result;
  }

  public RunningUserTaskInstanceImportIndexHandler getRunningUserTaskInstanceImportIndexHandler(String engineAlias) {
    RunningUserTaskInstanceImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(RunningUserTaskInstanceImportIndexHandler.class);
    }
    return result;
  }

  public IdentityLinkLogImportIndexHandler getIdentityLinkImportIndexHandler(String engineAlias) {
    IdentityLinkLogImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(IdentityLinkLogImportIndexHandler.class);
    }
    return result;
  }

  public List<EngineImportIndexHandler<?, ?>> getAllHandlers() {
    List<EngineImportIndexHandler<?, ?>> result = new ArrayList<>();
    for (EngineImportIndexHandlerProvider provider : engineImportIndexHandlerProviderMap.values()) {
      result.addAll(provider.getAllHandlers());
    }
    return result;
  }

  public ProcessDefinitionXmlImportIndexHandler getProcessDefinitionXmlImportIndexHandler(String engineAlias) {
    ProcessDefinitionXmlImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(ProcessDefinitionXmlImportIndexHandler.class);
    }
    return result;
  }

  public DecisionDefinitionImportIndexHandler getDecisionDefinitionImportIndexHandler(String engineAlias) {
    final EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    DecisionDefinitionImportIndexHandler result = null;
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(DecisionDefinitionImportIndexHandler.class);
    }
    return result;
  }

  public DecisionDefinitionXmlImportIndexHandler getDecisionDefinitionXmlImportIndexHandler(String engineAlias) {
    DecisionDefinitionXmlImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(DecisionDefinitionXmlImportIndexHandler.class);
    }
    return result;
  }

  public DecisionInstanceImportIndexHandler getDecisionInstanceImportIndexHandler(String engineAlias) {
    DecisionInstanceImportIndexHandler result = null;
    EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap.get(
      engineAlias);
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(DecisionInstanceImportIndexHandler.class);
    }
    return result;
  }

  public TenantImportIndexHandler getTenantImportIndexHandler(String engineAlias) {
    final EngineImportIndexHandlerProvider engineImportIndexHandlerProvider = engineImportIndexHandlerProviderMap
      .get(engineAlias);
    TenantImportIndexHandler result = null;
    if (engineImportIndexHandlerProvider != null) {
      result = engineImportIndexHandlerProvider.getImportIndexHandler(TenantImportIndexHandler.class);
    }
    return result;
  }

  public ZeebeProcessDefinitionImportIndexHandler getZeebeProcessDefinitionImportIndexHandler(Integer partitionId) {
    final ZeebeImportIndexHandlerProvider zeebeImportIndexHandlerProvider =
      zeebeImportIndexHandlerProviderMap.get(partitionId);
    ZeebeProcessDefinitionImportIndexHandler result = null;
    if (zeebeImportIndexHandlerProvider != null) {
      result = zeebeImportIndexHandlerProvider.getImportIndexHandler(ZeebeProcessDefinitionImportIndexHandler.class);
    }
    return result;
  }

  public ZeebeProcessInstanceImportIndexHandler getZeebeProcessInstanceImportIndexHandler(Integer partitionId) {
    final ZeebeImportIndexHandlerProvider zeebeImportIndexHandlerProvider =
      zeebeImportIndexHandlerProviderMap.get(partitionId);
    ZeebeProcessInstanceImportIndexHandler result = null;
    if (zeebeImportIndexHandlerProvider != null) {
      result = zeebeImportIndexHandlerProvider.getImportIndexHandler(ZeebeProcessInstanceImportIndexHandler.class);
    }
    return result;
  }

  public void reloadConfiguration() {
    this.engineImportIndexHandlerProviderMap = new HashMap<>();
    this.zeebeImportIndexHandlerProviderMap = new HashMap<>();
  }
}
