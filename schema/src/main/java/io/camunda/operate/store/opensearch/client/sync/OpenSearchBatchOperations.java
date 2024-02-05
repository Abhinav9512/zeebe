/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.operate.store.opensearch.client.sync;

import io.camunda.operate.exceptions.OperateRuntimeException;
import io.camunda.operate.exceptions.PersistenceException;
import io.camunda.operate.store.BatchRequest;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.util.List;

import static io.camunda.operate.util.ExceptionHelper.withOperateRuntimeException;

public class OpenSearchBatchOperations extends OpenSearchSyncOperation {
  private final BeanFactory beanFactory;

  public OpenSearchBatchOperations(Logger logger, OpenSearchClient openSearchClient, BeanFactory beanFactory) {
    super(logger, openSearchClient);
    this.beanFactory = beanFactory;
  }

  private void processBulkRequest(OpenSearchClient osClient, BulkRequest bulkRequest) throws PersistenceException {
    if (bulkRequest.operations().size() > 0) {
      try {
        logger.debug("************* FLUSH BULK START *************");
        final BulkResponse bulkItemResponses = osClient.bulk(bulkRequest);
        final List<BulkResponseItem> items = bulkItemResponses.items();
        for (BulkResponseItem responseItem : items) {
          if (responseItem.error() != null) {
            // TODO check how to log the error for OpenSearch;
            logger.error(
              String.format(
                "%s failed for type [%s] and id [%s]: %s",
                responseItem.operationType(),
                responseItem.index(),
                responseItem.id(),
                responseItem.error().reason()),
              "error on OpenSearch BulkRequest");
            throw new PersistenceException(
              "Operation failed: " + responseItem.error().reason(),
              new OperateRuntimeException(responseItem.error().reason()),
              Integer.valueOf(responseItem.id()));
          }
        }
        logger.debug("************* FLUSH BULK FINISH *************");
      } catch (IOException ex) {
        throw new PersistenceException(
          "Error when processing bulk request against OpenSearch: " + ex.getMessage(), ex);
      }
    }
  }

  public void bulk(BulkRequest.Builder bulkRequestBuilder) {
    bulk(bulkRequestBuilder.build());
  }

  public void bulk(BulkRequest  bulkRequest) {
    withOperateRuntimeException(() -> {
      processBulkRequest(openSearchClient, bulkRequest);
      return null;
    });
  }

  public BatchRequest newBatchRequest(){
    return beanFactory.getBean(BatchRequest.class);
  }
}