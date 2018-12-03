package org.camunda.optimize.service.es.report.flownode.duration;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.dto.engine.ProcessDefinitionEngineDto;
import org.camunda.optimize.dto.optimize.ReportConstants;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.MapProcessReportResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewOperation;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewProperty;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.test.it.rule.ElasticSearchIntegrationTestRule;
import org.camunda.optimize.test.it.rule.EmbeddedOptimizeRule;
import org.camunda.optimize.test.it.rule.EngineDatabaseRule;
import org.camunda.optimize.test.it.rule.EngineIntegrationRule;
import org.camunda.optimize.test.util.DateUtilHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.camunda.optimize.test.util.ReportDataBuilderHelper.createAverageFlowNodeDurationGroupByFlowNodeHeatmapReport;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;


public class AverageFlowNodeDurationByFlowNodeReportEvaluationIT {

  private static final String START_EVENT = "startEvent";
  private static final String END_EVENT = "endEvent";
  public static final String PROCESS_DEFINITION_KEY = "123";
  private static final String SERVICE_TASK_ID = "aSimpleServiceTask";
  private static final String SERVICE_TASK_ID_2 = "aSimpleServiceTask2";

  public EngineIntegrationRule engineRule = new EngineIntegrationRule();
  public ElasticSearchIntegrationTestRule elasticSearchRule = new ElasticSearchIntegrationTestRule();
  public EmbeddedOptimizeRule embeddedOptimizeRule = new EmbeddedOptimizeRule();
  public EngineDatabaseRule engineDatabaseRule = new EngineDatabaseRule();

  @Rule
  public RuleChain chain = RuleChain
      .outerRule(elasticSearchRule).around(engineRule).around(embeddedOptimizeRule).around(engineDatabaseRule);

  @Test
  public void reportEvaluationForOneProcess() throws Exception {

    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 20L);
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    MapProcessReportResultDto result = evaluateReport(reportData);

    // then
    ProcessReportDataDto resultReportDataDto = result.getData();
    assertThat(result.getProcessInstanceCount(), is(1L));
    assertThat(resultReportDataDto.getProcessDefinitionKey(), is(processDefinition.getKey()));
    assertThat(resultReportDataDto.getProcessDefinitionVersion(), is(String.valueOf(processDefinition.getVersion())));
    assertThat(resultReportDataDto.getView(), is(notNullValue()));
    assertThat(resultReportDataDto.getView().getOperation(), is(ProcessViewOperation.AVG));
    assertThat(resultReportDataDto.getView().getEntity(), is(ProcessViewEntity.FLOW_NODE));
    assertThat(resultReportDataDto.getView().getProperty(), is(ProcessViewProperty.DURATION));
    assertThat(result.getResult(), is(notNullValue()));
    Map<String, Long> flowNodeIdToAverageExecutionDuration = result.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration.size(), is(3));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID ), is(20L));
    assertThat(flowNodeIdToAverageExecutionDuration.get(START_EVENT ), is(20L));
    assertThat(flowNodeIdToAverageExecutionDuration.get(END_EVENT ), is(20L));
  }

  @Test
  public void reportEvaluationForSeveralProcesses() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 10L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 30L);
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    MapProcessReportResultDto result = evaluateReport(reportData);

    // then
    Map<String, Long> flowNodeIdToAverageExecutionDuration = result.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration.size(), is(3));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID ), is(20L));
  }

  @Test
  public void evaluateReportForMultipleEvents() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deployProcessWithTwoTasks();

    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 10L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 20L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 10L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 20L);
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData =
      getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    MapProcessReportResultDto result = evaluateReport(reportData);

    // then
    Map<String, Long> flowNodeIdToAverageExecutionDuration = result.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration.size(), is(4));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID ), is(10L));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID_2 ), is(20L));
  }

  private ProcessDefinitionEngineDto deployProcessWithTwoTasks() throws IOException {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess")
      .startEvent(START_EVENT)
      .serviceTask(SERVICE_TASK_ID)
        .camundaExpression("${true}")
      .serviceTask(SERVICE_TASK_ID_2)
        .camundaExpression("${true}")
      .endEvent(END_EVENT)
      .done();
    return engineRule.deployProcessAndGetProcessDefinition(modelInstance);
  }

  @Test
  public void allVersionsRespectLatestNodesOnlyWhereLatestHasMoreNodes() throws Exception {
    //given
    ProcessDefinitionEngineDto firstDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessDefinitionEngineDto latestDefinition = deployProcessWithTwoTasks();
    assertThat(latestDefinition.getVersion(), is(2));

    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 40L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 40L);

    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    //when
    ProcessReportDataDto reportData = createAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(
        latestDefinition.getKey(), ReportConstants.ALL_VERSIONS
    );
    MapProcessReportResultDto result = evaluateReport(reportData);

    //then
    Map<String, Long> flowNodeIdToAverageExecutionDuration = result.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration.size(), is(4));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID ), is(40L));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID_2 ), is(40L));
  }

  @Test
  public void allVersionsRespectLatestNodesOnlyWhereLatestHasLessNodes() throws Exception {
    //given
    ProcessDefinitionEngineDto firstDefinition = deployProcessWithTwoTasks();
    ProcessDefinitionEngineDto latestDefinition = deploySimpleServiceTaskProcessDefinition();
    assertThat(latestDefinition.getVersion(), is(2));

    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 40L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 40L);

    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    //when
    ProcessReportDataDto reportData = createAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(
        latestDefinition.getKey(), ReportConstants.ALL_VERSIONS
    );
    MapProcessReportResultDto result = evaluateReport(reportData);

    //then
    Map<String, Long> flowNodeIdToAverageExecutionDuration = result.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration.size(), is(3));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID ), is(40L));
  }

  @Test
  public void reportAcrossAllVersions() throws Exception {
    //given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessDefinitionEngineDto processDefinition2 = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 40L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition2.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 40L);

    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    //when
    ProcessReportDataDto reportData = createAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(
        processDefinition.getKey(), ReportConstants.ALL_VERSIONS
    );
    MapProcessReportResultDto result = evaluateReport(reportData);

    //then
    Map<String, Long> flowNodeIdToAverageExecutionDuration = result.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration.size(), is(3));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID ), is(40L));
  }

  @Test
  public void otherProcessDefinitionsDoNotInfluenceResult() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessDefinitionEngineDto processDefinition2 = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 40L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 40L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition2.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 20L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition2.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 20L);
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData1 = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    MapProcessReportResultDto result1 = evaluateReport(reportData1);
    ProcessReportDataDto reportData2 = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition2);
    MapProcessReportResultDto result2 = evaluateReport(reportData2);

    // then
    Map<String, Long> flowNodeIdToAverageExecutionDuration = result1.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration.size(), is(3));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID ), is(40L));
    Map<String, Long> flowNodeIdToAverageExecutionDuration2 = result2.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration2.size(), is(3));
    assertThat(flowNodeIdToAverageExecutionDuration2.get(SERVICE_TASK_ID ), is(20L));
  }

  @Test
  public void evaluateReportWithIrrationalAverageNumberAsResult() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 100L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 300L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 600L);
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    MapProcessReportResultDto result = evaluateReport(reportData);

    // then
    Map<String, Long> flowNodeIdToAverageExecutionDuration = result.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration.size(), is(3));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID ), is(333L));
  }

  @Test
  public void noEventMatchesReturnsEmptyResult() {

    // when
    ProcessReportDataDto reportData =
      createAverageFlowNodeDurationGroupByFlowNodeHeatmapReport("nonExistingProcessDefinitionId", "1");
    MapProcessReportResultDto result = evaluateReport(reportData);

    // then
    Map<String, Long> flowNodeIdToAverageExecutionDuration = result.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration.size(), is(0));
  }

  @Test
  public void processDefinitionContainsMultiInstanceBody() throws Exception {
    // given
    BpmnModelInstance subProcess = Bpmn.createExecutableProcess("subProcess")
        .startEvent()
          .serviceTask(SERVICE_TASK_ID)
            .camundaExpression("${true}")
        .endEvent()
        .done();

    BpmnModelInstance miProcess = Bpmn.createExecutableProcess("miProcess")
        .name("MultiInstance")
          .startEvent("miStart")
          .callActivity("callActivity")
            .calledElement("subProcess")
            .camundaIn("activityDuration", "activityDuration")
            .multiInstance()
              .cardinality("2")
            .multiInstanceDone()
          .endEvent("miEnd")
        .done();
    ProcessDefinitionEngineDto subProcessDefinition = engineRule.deployProcessAndGetProcessDefinition(subProcess);
    String processDefinitionId = engineRule.deployProcessAndGetId(miProcess);
    engineRule.startProcessInstance(processDefinitionId);
    engineDatabaseRule.changeActivityDurationForProcessDefinition(subProcessDefinition.getId(), 10L);
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData =
      createAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(subProcessDefinition.getKey(), String.valueOf(subProcessDefinition.getVersion()));
    MapProcessReportResultDto result = evaluateReport(reportData);

    // then
    Map<String, Long> flowNodeIdToAverageExecutionDuration = result.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration.size(), is(3));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID ), is(10L));
  }

  @Test
  public void evaluateReportForMoreThenTenEvents() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();

    ProcessInstanceEngineDto processInstanceDto;
    for (int i = 0; i < 11; i++) {
      processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
      engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 10L);
    }
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    MapProcessReportResultDto result = evaluateReport(reportData);

    // then
    Map<String, Long> flowNodeIdToAverageExecutionDuration = result.getResult();
    assertThat(flowNodeIdToAverageExecutionDuration.size(), is(3));
    assertThat(flowNodeIdToAverageExecutionDuration.get(SERVICE_TASK_ID ), is(10L));
  }

  @Test
  public void filterInReport() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstance = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstance.getId(), 10L);
    OffsetDateTime past = engineRule.getHistoricProcessInstance(processInstance.getId()).getStartTime();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    reportData.setFilter(createStartDateFilter(null, past.minusSeconds(1L)));
    MapProcessReportResultDto result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(notNullValue()));
    Map<String, Long> flowNodeIdToExecutionFrequency = result.getResult();
    assertThat(flowNodeIdToExecutionFrequency.size(), is(0));

    // when
    reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    reportData.setFilter(createStartDateFilter(past, null));
    result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(notNullValue()));
    flowNodeIdToExecutionFrequency = result.getResult();
    assertThat(flowNodeIdToExecutionFrequency.size(), is(3));
    assertThat(flowNodeIdToExecutionFrequency.get(SERVICE_TASK_ID ), is(10L));
  }

  private List<ProcessFilterDto> createStartDateFilter(OffsetDateTime startDate, OffsetDateTime endDate) {
    return DateUtilHelper.createFixedStartDateFilter(startDate, endDate);
  }

  @Test
  public void optimizeExceptionOnViewEntityIsNull() {
    // given
    ProcessReportDataDto dataDto =
      createAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getView().setEntity(null);

    //when
    Response response = evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus(), is(500));
  }

  @Test
  public void optimizeExceptionOnViewPropertyIsNull() {
    // given
    ProcessReportDataDto dataDto =
      createAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getView().setProperty(null);

    //when
    Response response = evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus(), is(500));
  }

  @Test
  public void optimizeExceptionOnGroupByTypeIsNull() {
    // given
    ProcessReportDataDto dataDto =
      createAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getGroupBy().setType(null);

    //when
    Response response = evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus(), is(400));
  }

  private ProcessDefinitionEngineDto deploySimpleServiceTaskProcessDefinition() throws IOException {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess" )
      .startEvent(START_EVENT)
      .serviceTask(SERVICE_TASK_ID)
        .camundaExpression("${true}")
      .endEvent(END_EVENT)
      .done();
    return engineRule.deployProcessAndGetProcessDefinition(modelInstance);
  }

  private MapProcessReportResultDto evaluateReport(ProcessReportDataDto reportData) {
    Response response = evaluateReportAndReturnResponse(reportData);
    assertThat(response.getStatus(), is(200));

    return response.readEntity(MapProcessReportResultDto.class);
  }

  private Response evaluateReportAndReturnResponse(ProcessReportDataDto reportData) {
    return embeddedOptimizeRule
            .getRequestExecutor()
            .buildEvaluateSingleUnsavedReportRequest(reportData)
            .execute();
  }

  private ProcessReportDataDto getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(ProcessDefinitionEngineDto processDefinition) {
    return createAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition.getKey(), String.valueOf(processDefinition.getVersion()));
  }

}
