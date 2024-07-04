/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.reader;

import io.camunda.optimize.dto.optimize.query.collection.CollectionDefinitionDto;
import java.util.List;
import java.util.Optional;

public interface CollectionReader {

  Optional<CollectionDefinitionDto> getCollection(String collectionId);

  List<CollectionDefinitionDto> getAllCollections();
}