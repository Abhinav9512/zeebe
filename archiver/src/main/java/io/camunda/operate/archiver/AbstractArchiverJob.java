/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.operate.archiver;

import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.util.BackoffIdleStrategy;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractArchiverJob implements ArchiverJob {

  private static final Logger logger = LoggerFactory.getLogger(AbstractArchiverJob.class);

  public static final String DATES_AGG = "datesAgg";
  public static final String INSTANCES_AGG = "instancesAgg";

  private final BackoffIdleStrategy idleStrategy;
  private final BackoffIdleStrategy errorStrategy;

  private boolean shutdown = false;

  @Autowired
  @Qualifier("archiverThreadPoolExecutor")
  protected ThreadPoolTaskScheduler archiverExecutor;

  @Autowired
  private OperateProperties operateProperties;

  public AbstractArchiverJob() {
    this.idleStrategy = new BackoffIdleStrategy(2_000, 1.2f, 60_000);
    this.errorStrategy = new BackoffIdleStrategy(100, 1.2f, 10_000);
  }

  @Override
  public void run() {

    archiveNextBatch()
      .thenApply((count) -> {
        errorStrategy.reset();

        if (count >= operateProperties.getArchiver().getRolloverBatchSize()) {
          idleStrategy.reset();
        } else {
          idleStrategy.idle();
        }

        final var delay = Math.max(
            operateProperties.getArchiver().getDelayBetweenRuns(),
            idleStrategy.idleTime());

        return delay;
      })
      .exceptionally((t) -> {
        logger.error("Error occurred while archiving data. Will be retried.", t);
        errorStrategy.idle();
        final var delay = Math.max(
            operateProperties.getArchiver().getDelayBetweenRuns(),
            errorStrategy.idleTime());
        return delay;
      })
      .thenAccept((delay) -> {
        if (!shutdown) {
          archiverExecutor.schedule(this, Date.from(Instant.now().plusMillis(delay)));
        }
      });
  }

  @Override
  public CompletableFuture<Integer> archiveNextBatch() {
    return getNextBatch().thenCompose(this::archiveBatch);
  }

  @PreDestroy
  public void shutdown() {
    shutdown = true;
  }
}
