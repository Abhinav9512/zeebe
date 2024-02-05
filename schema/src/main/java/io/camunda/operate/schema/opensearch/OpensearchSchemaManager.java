/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.operate.schema.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.operate.conditions.OpensearchCondition;
import io.camunda.operate.exceptions.OperateRuntimeException;
import io.camunda.operate.property.OperateOpensearchProperties;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.schema.SchemaManager;
import io.camunda.operate.schema.indices.IndexDescriptor;
import io.camunda.operate.schema.templates.TemplateDescriptor;
import io.camunda.operate.store.opensearch.client.sync.RichOpenSearchClient;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonParser;
import org.opensearch.client.json.jsonb.JsonbJsonpMapper;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.cluster.PutComponentTemplateRequest;
import org.opensearch.client.opensearch.indices.Alias;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.PutIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.put_index_template.IndexTemplateMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.*;

import static java.lang.String.format;

@Component("schemaManager")
@Profile("!test")
@Conditional(OpensearchCondition.class)
public class OpensearchSchemaManager implements SchemaManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpensearchSchemaManager.class);
  public static final String SCHEMA_OPENSEARCH_CREATE_TEMPLATE_JSON = "/schema/opensearch/create/template/operate-%s.json";
  public static final String SCHEMA_OPENSEARCH_CREATE_INDEX_JSON = "/schema/opensearch/create/index/operate-%s.json";
  public static final String SCHEMA_OPENSEARCH_CREATE_POLICY_JSON = "/schema/opensearch/create/policy/%s.json";
  public static final String SETTINGS = "settings";
  public static final String MAPPINGS = "mappings";

  protected final OperateProperties operateProperties;

  protected final RichOpenSearchClient richOpenSearchClient;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonbJsonpMapper jsonpMapper = new JsonbJsonpMapper();

  private final List<TemplateDescriptor> templateDescriptors;

  private final List<IndexDescriptor> indexDescriptors;

  @Autowired
  public OpensearchSchemaManager(final OperateProperties operateProperties, final RichOpenSearchClient richOpenSearchClient,
      final List<TemplateDescriptor> templateDescriptors, final List<IndexDescriptor> indexDescriptors){
    super();
    this.operateProperties = operateProperties;
    this.richOpenSearchClient = richOpenSearchClient;
    this.templateDescriptors = templateDescriptors;
    this.indexDescriptors = indexDescriptors.stream().filter( indexDescriptor -> !(indexDescriptor instanceof TemplateDescriptor)).toList();
  }
  @Override
  public void createSchema() {
    if (operateProperties.getArchiver().isIlmEnabled()) {
      createIsmPolicy();
    }
    createDefaults();
    createTemplates();
    createIndices();
  }

  @Override
  public boolean setIndexSettingsFor(Map<String, ?> settings, String indexPattern) {
    IndexSettings indexSettings =  new IndexSettings.Builder()
        .refreshInterval( ri -> ri.time(((String)settings.get(REFRESH_INTERVAL))))
        .numberOfReplicas(String.valueOf(settings.get(NUMBERS_OF_REPLICA)))
        .build();
    return richOpenSearchClient.index().setIndexSettingsFor(indexSettings, indexPattern);
  }

  @Override
  public String getOrDefaultRefreshInterval(String indexName, String defaultValue) {
    return richOpenSearchClient.index().getOrDefaultRefreshInterval(indexName, defaultValue);
  }

  @Override
  public String getOrDefaultNumbersOfReplica(String indexName, String defaultValue) {
    return richOpenSearchClient.index().getOrDefaultNumbersOfReplica(indexName, defaultValue);
  }

  @Override
  public void refresh(String indexPattern) {
    richOpenSearchClient.index().refreshWithRetries(indexPattern);
  }

  @Override
  public boolean isHealthy() {
    return richOpenSearchClient.cluster().isHealthy();
  }

  @Override
  public Set<String> getIndexNames(String indexPattern) {
    return richOpenSearchClient.index().getIndexNamesWithRetries(indexPattern);
  }

  @Override
  public Set<String> getAliasesNames(String indexPattern) {
    return richOpenSearchClient.index().getAliasesNamesWithRetries(indexPattern);
  }

  @Override
  public long getNumberOfDocumentsFor(String... indexPatterns) {
    return richOpenSearchClient.index().getNumberOfDocumentsWithRetries(indexPatterns);
  }

  @Override
  public boolean deleteIndicesFor(String indexPattern) {
    return richOpenSearchClient.index().deleteIndicesWithRetries(indexPattern);
  }

  @Override
  public boolean deleteTemplatesFor(String deleteTemplatePattern) {
    return richOpenSearchClient.template().deleteTemplatesWithRetries(deleteTemplatePattern);
  }

  @Override
  public void removePipeline(String pipelineName) {
    richOpenSearchClient.pipeline().removePipelineWithRetries(pipelineName);
  }

  @Override
  public boolean addPipeline(String name, String pipelineDefinition) {
    return richOpenSearchClient.pipeline().addPipelineWithRetries(name, pipelineDefinition);
  }

  @Override
  public Map<String, String> getIndexSettingsFor(String indexName, String... fields) {
    IndexSettings indexSettings =  richOpenSearchClient.index().getIndexSettingsWithRetries(indexName);
    var result = new HashMap<String,String>();
    for (String field: fields){
      if(field.equals(REFRESH_INTERVAL)){
        var refreshInterval = indexSettings.refreshInterval();
        result.put(REFRESH_INTERVAL, refreshInterval!=null?refreshInterval.time():null);
      }
      if(field.equals(NUMBERS_OF_REPLICA)){
        result.put(NUMBERS_OF_REPLICA, indexSettings.numberOfReplicas());
      }
    }
    return result;
  }


  private void createDefaults() {
    final OperateOpensearchProperties osConfig = operateProperties.getOpensearch();

    final String settingsTemplateName = settingsTemplateName();
    LOGGER.info(
        "Create default settings '{}' with {} shards and {} replicas per index.",
        settingsTemplateName,
        osConfig.getNumberOfShards(),
        osConfig.getNumberOfReplicas());

    final IndexSettings settings = getDefaultIndexSettings();
    richOpenSearchClient.template().createComponentTemplateWithRetries(
        new PutComponentTemplateRequest.Builder()
            .name(settingsTemplateName)
            .template(t -> t.settings(settings))
            .build());
  }


  private IndexSettings getDefaultIndexSettings() {
    final OperateOpensearchProperties osConfig = operateProperties.getOpensearch();
    return new IndexSettings.Builder()
      .numberOfShards(String.valueOf(osConfig.getNumberOfShards()))
      .numberOfReplicas(String.valueOf(osConfig.getNumberOfReplicas()))
      .build();
  }

  private IndexSettings getIndexSettings(String indexName) {
    final OperateOpensearchProperties osConfig = operateProperties.getOpensearch();
    var shards = osConfig.getNumberOfShardsForIndices().getOrDefault(indexName, osConfig.getNumberOfShards());
    var replicas = osConfig.getNumberOfReplicasForIndices().getOrDefault(indexName, osConfig.getNumberOfReplicas());

    return new IndexSettings.Builder()
        .numberOfShards(String.valueOf(shards))
        .numberOfReplicas(String.valueOf(replicas))
        .build();
  }

  private String settingsTemplateName() {
    final OperateOpensearchProperties osConfig = operateProperties.getOpensearch();
    return format("%s_template", osConfig.getIndexPrefix());
  }

  private void createTemplates() {
    templateDescriptors.forEach(this::createTemplate);
  }

  private IndexSettings templateSettings(final TemplateDescriptor templateDescriptor) {
    var shards = operateProperties.getOpensearch()
      .getNumberOfShardsForIndices()
      .get(templateDescriptor.getIndexName());

    var replicas = operateProperties.getOpensearch()
      .getNumberOfReplicasForIndices()
      .get(templateDescriptor.getIndexName());

    if(shards != null || replicas != null) {
      var indexSettingsBuilder = new IndexSettings.Builder();

      if(shards != null) {
        indexSettingsBuilder.numberOfShards(shards.toString());
      }

      if(replicas != null) {
        indexSettingsBuilder.numberOfReplicas(replicas.toString());
      }

      return indexSettingsBuilder.build();
    }

    return null;
  }

  private void createTemplate(final TemplateDescriptor templateDescriptor) {
    var templateSettings = templateSettings(templateDescriptor);
    var templateBuilder = new IndexTemplateMapping.Builder()
      .aliases(templateDescriptor.getAlias(), new Alias.Builder().build());

    if(templateSettings != null) {
      templateBuilder.settings(templateSettings);
    }

    final IndexTemplateMapping template = templateBuilder.build();

    putIndexTemplate(
        new PutIndexTemplateRequest.Builder()
            .name(templateDescriptor.getTemplateName())
            .indexPatterns(templateDescriptor.getIndexPattern())
            .template(template)
            .composedOf(settingsTemplateName())
            .build());

    // This is necessary, otherwise operate won't find indexes at startup
    final String indexName = templateDescriptor.getFullQualifiedName();
    final String templateFileName = format(SCHEMA_OPENSEARCH_CREATE_TEMPLATE_JSON, templateDescriptor.getIndexName());
    try {
      final InputStream description = OpensearchSchemaManager.class.getResourceAsStream(templateFileName);
      var request = createIndexFromJson(
          StreamUtils.copyToString(description, Charset.defaultCharset()),
          templateDescriptor.getFullQualifiedName(),
          Map.of(templateDescriptor.getAlias(), new Alias.Builder().isWriteIndex(false).build()),
          getIndexSettings(templateDescriptor.getIndexName())
      );
      createIndex(request, indexName);
    }catch (Exception e){
      throw new OperateRuntimeException(e);
    }
  }

  private void putIndexTemplate(final PutIndexTemplateRequest request) {
    final boolean created = richOpenSearchClient.template().createTemplateWithRetries(request);
    if (created) {
      LOGGER.debug("Template [{}] was successfully created", request.name());
    } else {
      LOGGER.debug("Template [{}] was NOT created", request.name());
    }
  }

  private void createIndex(final CreateIndexRequest createIndexRequest, String indexName) {
    final boolean created = richOpenSearchClient.index().createIndexWithRetries(createIndexRequest);
    if (created) {
      LOGGER.debug("Index [{}] was successfully created", indexName);
    } else {
      LOGGER.debug("Index [{}] was NOT created", indexName);
    }
  }

  private void createIndex(final IndexDescriptor indexDescriptor)  {
    try {
      final String indexFilename = format(SCHEMA_OPENSEARCH_CREATE_INDEX_JSON, indexDescriptor.getIndexName());
      final InputStream description = OpensearchSchemaManager.class.getResourceAsStream(indexFilename);
      var request = createIndexFromJson(
          StreamUtils.copyToString(description, Charset.defaultCharset()),
          indexDescriptor.getFullQualifiedName(),
          Map.of(indexDescriptor.getAlias(), new Alias.Builder().isWriteIndex(false).build()),
          getIndexSettings(indexDescriptor.getIndexName())
      );
      createIndex(request, indexDescriptor.getFullQualifiedName());
    }catch (Exception e){
      throw new OperateRuntimeException("Could not create index "+indexDescriptor.getIndexName(), e);
    }
  }

  /**
   *  Reads mappings and optionally settings from json file
   */
  private CreateIndexRequest createIndexFromJson(String json, String indexName, Map<String, Alias> aliases, IndexSettings settings) {
    try {
      var indexAsJSONNode = objectMapper.readTree(new StringReader(json));

      var customSettings = getCustomSettings(settings, indexAsJSONNode);
      var mappings = getMappings(indexAsJSONNode.get(MAPPINGS));

      return new CreateIndexRequest.Builder()
          .index(indexName)
          .aliases(aliases)
          .settings(customSettings)
          .mappings(mappings)
          .build();
    } catch (Exception e){
      throw new OperateRuntimeException("Could not load schema for " + indexName, e);
    }
  }

  private TypeMapping getMappings(JsonNode mappingsAsJSON) {
    JsonParser jsonParser = JsonProvider.provider().createParser(new StringReader(mappingsAsJSON.toPrettyString()));
    return TypeMapping._DESERIALIZER.deserialize(jsonParser, jsonpMapper);
  }

  private IndexSettings getCustomSettings(IndexSettings defaultSettings, JsonNode indexAsJSONNode) {
    if (indexAsJSONNode.has(SETTINGS)) {
      var settingsJSON = indexAsJSONNode.get(SETTINGS);
      JsonParser jsonParser = JsonProvider.provider().createParser(new StringReader(settingsJSON.toPrettyString()));
      var updatedSettings = IndexSettings._DESERIALIZER.deserialize(jsonParser, jsonpMapper);
      return new IndexSettings.Builder().index(defaultSettings).analysis(updatedSettings.analysis()).build();
    }
    return defaultSettings;
  }

  private void createIndices() {
    indexDescriptors.forEach(this::createIndex);
  }

  @Override
  public String getIndexPrefix() {
    return operateProperties.getOpensearch().getIndexPrefix();
  }

  private Optional<Map<String, Object>> fetchIsmPolicy() {
    try {
      return Optional.ofNullable(richOpenSearchClient.ism().getPolicy(OPERATE_DELETE_ARCHIVED_INDICES));
    } catch (OpenSearchException e) {
      if(e.status() != 404) {
        LOGGER.error(format("Failed to get policy %s", OPERATE_DELETE_ARCHIVED_INDICES), e);
      }
      return Optional.empty();
    }
  }

  private String loadIsmPolicy() throws IOException {
    var policyFilename = format(SCHEMA_OPENSEARCH_CREATE_POLICY_JSON, OPERATE_DELETE_ARCHIVED_INDICES);
    var inputStream = OpensearchSchemaManager.class.getResourceAsStream(policyFilename);
    var policyContent = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
    return policyContent.replace("$MIN_INDEX_AGE", operateProperties.getArchiver().getIlmMinAgeForDeleteArchivedIndices());
  }

  private void createIsmPolicy() {
    fetchIsmPolicy().ifPresentOrElse(
      ismPolicy -> LOGGER.warn("ISM policy {} already exists: {}.", OPERATE_DELETE_ARCHIVED_INDICES, ismPolicy),
      () -> {
        try {
          richOpenSearchClient.ism().createPolicy(OPERATE_DELETE_ARCHIVED_INDICES, loadIsmPolicy());
          LOGGER.info("Created ISM policy {} for min age of {}.", OPERATE_DELETE_ARCHIVED_INDICES, operateProperties.getArchiver().getIlmMinAgeForDeleteArchivedIndices());
        } catch(Exception e){
          throw new OperateRuntimeException("Failed to create ISM policy " + OPERATE_DELETE_ARCHIVED_INDICES, e);
        }
      }
    );
  }
}