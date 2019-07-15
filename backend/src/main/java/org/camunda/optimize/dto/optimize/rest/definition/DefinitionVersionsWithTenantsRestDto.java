/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

package org.camunda.optimize.dto.optimize.rest.definition;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.camunda.optimize.dto.optimize.rest.TenantRestDto;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Data
public class DefinitionVersionsWithTenantsRestDto {
  @NonNull
  private String key;
  private String name;
  @NonNull
  private List<DefinitionVersionsRestDto> versions;
  private List<TenantRestDto> tenants;
}
