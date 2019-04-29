/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.engine.importing.index.handler.impl;

import org.camunda.optimize.dto.engine.TenantEngineDto;
import org.camunda.optimize.rest.engine.EngineContext;
import org.camunda.optimize.service.engine.importing.index.handler.AllEntitiesBasedImportIndexHandler;
import org.camunda.optimize.upgrade.es.ElasticsearchConstants;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TenantImportIndexHandler extends AllEntitiesBasedImportIndexHandler {

  private Map<String, TenantEngineDto> alreadyImportedTenants = new HashMap<>();

  public TenantImportIndexHandler(EngineContext engineContext) {
    super(engineContext);
  }

  @Override
  protected String getElasticsearchImportIndexType() {
    return ElasticsearchConstants.TENANT_TYPE;
  }

  public void addImportedTenants(final Collection<TenantEngineDto> tenantDtos) {
    tenantDtos.forEach(tenant -> alreadyImportedTenants.put(tenant.getId(), tenant));
    moveImportIndex(tenantDtos.size());
  }

  public List<TenantEngineDto> filterNewOrChangedTenants(final List<TenantEngineDto> engineEntities) {
    final Collection<TenantEngineDto> importedTenantDtos = alreadyImportedTenants.values();
    return engineEntities
      .stream()
      .filter(tenantDto -> !importedTenantDtos.contains(tenantDto))
      .collect(Collectors.toList());
  }

  @Override
  public void resetImportIndex() {
    super.resetImportIndex();
    alreadyImportedTenants.clear();
  }
}
