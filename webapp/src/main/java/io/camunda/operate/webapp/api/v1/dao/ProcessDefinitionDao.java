/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.camunda.operate.webapp.api.v1.dao;

import io.camunda.operate.webapp.api.v1.entities.ProcessDefinition;
import io.camunda.operate.webapp.api.v1.exceptions.APIException;
import io.camunda.operate.webapp.api.v1.entities.Query;
import io.camunda.operate.webapp.api.v1.entities.Results;

public interface ProcessDefinitionDao {

  Results<ProcessDefinition> listBy(final Query<ProcessDefinition> query) throws APIException;

  ProcessDefinition byKey(Long key) throws APIException;

  String xmlByKey(Long key) throws APIException;
}
