/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React, {runAllEffects} from 'react';
import {shallow} from 'enzyme';

import {getWebappEndpoints} from 'config';
import {LoadingIndicator} from 'components';

import RawDataTable from './RawDataTable';
import processRawData from './processRawData';
import ObjectVariableModal from './ObjectVariableModal';

jest.mock('./processRawData', () => jest.fn().mockReturnValue({}));

jest.mock('config', () => ({getWebappEndpoints: jest.fn()}));

jest.mock('services', () => {
  return {
    ...jest.requireActual('services'),
    loadVariables: jest.fn().mockReturnValue([]),
  };
});

beforeEach(() => {
  jest.clearAllMocks();
});

const testDefinition = {key: 'definitionKey', versions: ['ver1'], tenantIds: ['id1']};
const report = {
  reportType: 'process',
  combined: false,
  data: {
    definitions: [testDefinition],
    groupBy: {
      value: {},
      type: '',
    },
    view: {properties: ['rawData']},
    configuration: {
      tableColumns: {
        includeNewVariables: true,
        includedColumns: [],
        excludedColumns: [],
        columnOrder: ['col1', 'col2', 'col3'],
      },
    },
    visualization: 'table',
  },
  result: {data: [1, 2, 3], pagination: {limit: 20}},
};
const props = {
  mightFail: jest.fn().mockImplementation((a, b) => b(a)),
  error: '',
  report,
};

it('should get the camunda endpoints for raw data', () => {
  getWebappEndpoints.mockClear();
  shallow(<RawDataTable {...props} />);
  runAllEffects();

  expect(getWebappEndpoints).toHaveBeenCalled();
});

it('should process raw data', () => {
  shallow(<RawDataTable {...props} />);
  runAllEffects();

  expect(processRawData).toHaveBeenCalled();
});

it('should display object variable modal when invoked from processRawData function', () => {
  const node = shallow(<RawDataTable {...props} />);
  runAllEffects();

  processRawData.mock.calls[0][0].onVariableView('varName', 'instanceId', testDefinition.key);

  expect(node.find(ObjectVariableModal).prop('variable')).toEqual({
    name: 'varName',
    processInstanceId: 'instanceId',
    processDefinitionKey: testDefinition.key,
    versions: testDefinition.versions,
    tenantIds: testDefinition.tenantIds,
  });

  node.find(ObjectVariableModal).simulate('close');
  expect(node.find(ObjectVariableModal)).not.toExist();
});

it('should close object variable modal', () => {
  const node = shallow(<RawDataTable {...props} />);
  runAllEffects();

  processRawData.mock.calls[0][0].onVariableView('varName', 'instanceId', testDefinition.key);

  node.find(ObjectVariableModal).simulate('close');
  expect(node.find(ObjectVariableModal)).not.toExist();
});

it('should reload report with correct pagination parameters', () => {
  const spy = jest.fn();
  const node = shallow(<RawDataTable {...props} loadReport={spy} />);
  runAllEffects();

  node.find('Table').prop('fetchData')({pageIndex: 2, pageSize: 50});
  expect(spy).toHaveBeenCalledWith({limit: 50, offset: 100});
});

it('should show an error when loading more than the first 10.000 instances', async () => {
  const spy = jest.fn();
  const node = shallow(
    <RawDataTable
      {...props}
      loadReport={spy}
      report={{
        ...report,
        result: {data: [1, 2, 3], pagination: {limit: 1000}},
      }}
    />
  );
  runAllEffects();

  await node.find('Table').prop('fetchData')({pageIndex: 11, pageSize: 1000});
  expect(node.find('Table').prop('errorInPage')).toBeDefined();
  expect(spy).not.toHaveBeenCalled();
});

it('should update configuration when arranging columns', () => {
  processRawData.mockReturnValueOnce({head: ['col1', 'col2', {id: 'col3'}]});
  const spy = jest.fn();
  const node = shallow(<RawDataTable {...props} updateReport={spy} />);

  runAllEffects();

  node.find('ColumnRearrangement').prop('onChange')(0, 2);

  expect(spy).toHaveBeenCalledWith({
    configuration: {tableColumns: {columnOrder: {$set: ['col2', 'col3', 'col1']}}},
  });
});

it('should show loading indicator when loading camunda endpoints', () => {
  getWebappEndpoints.mockReturnValueOnce(null);
  const node = shallow(<RawDataTable {...props} />);

  runAllEffects();

  expect(node.find(LoadingIndicator)).toBeDefined();
});