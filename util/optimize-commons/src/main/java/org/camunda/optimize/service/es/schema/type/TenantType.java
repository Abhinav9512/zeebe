/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.schema.type;

import org.camunda.optimize.dto.optimize.persistence.TenantDto;
import org.camunda.optimize.service.es.schema.StrictTypeMappingCreator;
import org.camunda.optimize.upgrade.es.ElasticsearchConstants;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TenantType extends StrictTypeMappingCreator {
  public static final int VERSION = 1;

  @Override
  public String getType() {
    return ElasticsearchConstants.TENANT_TYPE;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public XContentBuilder addProperties(XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    return xContentBuilder
      .startObject(TenantDto.Fields.id.name())
        .field("type", "text")
        .field("index", false)
      .endObject()
      .startObject(TenantDto.Fields.name.name())
        .field("type", "text")
        .field("index", false)
      .endObject()
      .startObject(TenantDto.Fields.engine.name())
        .field("type", "text")
        .field("index", false)
      .endObject()
      ;
    // @formatter:on
  }

}
