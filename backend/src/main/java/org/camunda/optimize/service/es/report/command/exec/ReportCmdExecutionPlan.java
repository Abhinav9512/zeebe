/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.command.exec;

import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.SingleReportResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.SingleReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.configuration.sorting.SortingDto;
import org.camunda.optimize.service.es.OptimizeElasticsearchClient;
import org.camunda.optimize.service.es.report.command.CommandContext;
import org.camunda.optimize.service.es.report.command.modules.distributed_by.DistributedByPart;
import org.camunda.optimize.service.es.report.command.modules.group_by.GroupByPart;
import org.camunda.optimize.service.es.report.command.modules.result.CompositeCommandResult;
import org.camunda.optimize.service.es.report.command.modules.view.ViewPart;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public abstract class ReportCmdExecutionPlan<R extends SingleReportResultDto, Data extends SingleReportDataDto> {

  protected ViewPart<Data> viewPart;
  protected GroupByPart<Data> groupByPart;
  protected DistributedByPart<Data> distributedByPart;
  protected OptimizeElasticsearchClient esClient;
  protected Function<CompositeCommandResult, R> mapToReportResult;

  public ReportCmdExecutionPlan(final ViewPart<Data> viewPart,
                                final GroupByPart<Data> groupByPart,
                                final DistributedByPart<Data> distributedByPart,
                                final Function<CompositeCommandResult, R> mapToReportResult,
                                final OptimizeElasticsearchClient esClient) {
    groupByPart.setDistributedByPart(distributedByPart);
    distributedByPart.setViewPart(viewPart);
    this.viewPart = viewPart;
    this.groupByPart = groupByPart;
    this.distributedByPart = distributedByPart;
    this.mapToReportResult = mapToReportResult;
    this.esClient = esClient;
  }

  protected abstract BoolQueryBuilder setupBaseQuery(final Data reportData);

  protected abstract String getIndexName();

  public <T extends ReportDefinitionDto<Data>> R evaluate(final CommandContext<T> commandContext) {
    return evaluate(new ExecutionContext<>(commandContext));
  }

  protected R evaluate(final ExecutionContext<Data> executionContext) {
    final Data reportData = executionContext.getReportData();

    final BoolQueryBuilder baseQuery = setupBaseQuery(reportData);
    groupByPart.adjustBaseQuery(baseQuery, reportData);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(baseQuery)
      .fetchSource(false)
      .size(0);
    addAggregation(searchSourceBuilder, executionContext);

    SearchRequest searchRequest = new SearchRequest(getIndexName())
      .types(getIndexName())
      .source(searchSourceBuilder);

    SearchResponse response;
    try {
      response = esClient.search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String reason =
        String.format(
          "Could not evaluate %s %s %s report " +
            "for definition with key [%s] and versions [%s]",
          viewPart.getClass().getSimpleName(),
          groupByPart.getClass().getSimpleName(),
          distributedByPart.getClass().getSimpleName(),
          reportData.getDefinitionKey(),
          reportData.getDefinitionVersions()
        );
      log.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }

    return retrieveQueryResult(response, executionContext);
  }

  public String generateCommandKey() {
    return groupByPart.generateCommandKey(getDataDtoSupplier());
  }

  protected abstract Supplier<Data> getDataDtoSupplier();

  private R retrieveQueryResult(final SearchResponse response, final ExecutionContext<Data> executionContext) {
    final CompositeCommandResult result = groupByPart.retrieveQueryResult(response, executionContext.getReportData());
    final R reportResult = mapToReportResult.apply(result);
    reportResult.setInstanceCount(response.getHits().getTotalHits());
    final Optional<SortingDto> sorting = groupByPart.getSorting(executionContext);
    sorting.ifPresent(
      sortingDto -> reportResult.sortResultData(sortingDto, groupByPart.getSortByKeyIsOfNumericType(executionContext))
    );
    return reportResult;
  }

  private void addAggregation(final SearchSourceBuilder searchSourceBuilder,
                              final ExecutionContext<Data> executionContext) {
    final List<AggregationBuilder> aggregations = groupByPart.createAggregation(searchSourceBuilder, executionContext);
    aggregations.forEach(searchSourceBuilder::aggregation);
  }

}
