/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package org.camunda.optimize.service.db.os.writer;

import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.ImportRequestDto;
import org.camunda.optimize.dto.optimize.ProcessInstanceDto;
import org.camunda.optimize.service.db.writer.ZeebeProcessInstanceWriter;
import org.camunda.optimize.service.util.configuration.condition.OpenSearchCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@Conditional(OpenSearchCondition.class)
public class ZeebeProcessInstanceWriterOS implements ZeebeProcessInstanceWriter {
  @Override
  public List<ImportRequestDto> generateProcessInstanceImports(final List<ProcessInstanceDto> processInstances,
                                                               final String sourceExportIndex) {
    log.error("Functionality not implemented for OpenSearch");
    return null;
  }

}