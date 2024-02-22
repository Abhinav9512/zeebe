/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.operate.property;

import java.util.Map;

public class OperateElasticsearchProperties extends ElasticsearchProperties {

  public static final String DEFAULT_INDEX_PREFIX = "operate";
  private static final int DEFAULT_NUMBER_OF_SHARDS = 1;
  private static final int DEFAULT_NUMBER_OF_REPLICAS = 0;
  private static final String DEFAULT_REFRESH_INTERVAL = "1s";

  private String indexPrefix = DEFAULT_INDEX_PREFIX;
  private int numberOfShards = DEFAULT_NUMBER_OF_SHARDS;
  private Map<String, Integer> numberOfShardsForIndices = Map.of();
  private int numberOfReplicas = DEFAULT_NUMBER_OF_REPLICAS;
  private Map<String, Integer> numberOfReplicasForIndices = Map.of();
  private String refreshInterval = DEFAULT_REFRESH_INTERVAL;

  public String getIndexPrefix() {
    return indexPrefix;
  }

  public void setIndexPrefix(String indexPrefix) {
    this.indexPrefix = indexPrefix;
  }

  public void setDefaultIndexPrefix() {
    setIndexPrefix(DEFAULT_INDEX_PREFIX);
  }

  public int getNumberOfShards() {
    return numberOfShards;
  }

  public void setNumberOfShards(final int numberOfShards) {
    this.numberOfShards = numberOfShards;
  }

  public int getNumberOfReplicas() {
    return numberOfReplicas;
  }

  public void setNumberOfReplicas(final int numberOfReplicas) {
    this.numberOfReplicas = numberOfReplicas;
  }

  public String getRefreshInterval() {
    return refreshInterval;
  }

  public void setRefreshInterval(String refreshInterval) {
    this.refreshInterval = refreshInterval;
  }

  public Map<String, Integer> getNumberOfShardsForIndices() {
    return numberOfShardsForIndices;
  }

  public void setNumberOfShardsForIndices(Map<String, Integer> numberOfShardsForIndices) {
    this.numberOfShardsForIndices = numberOfShardsForIndices;
  }

  public Map<String, Integer> getNumberOfReplicasForIndices() {
    return numberOfReplicasForIndices;
  }

  public void setNumberOfReplicasForIndices(Map<String, Integer> numberOfReplicasForIndices) {
    this.numberOfReplicasForIndices = numberOfReplicasForIndices;
  }
}
