/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React, {runAllEffects} from 'react';
import {shallow} from 'enzyme';

import {Select, Button} from 'components';
import {reportConfig, updateReport} from 'services';
import {isOptimizeCloudEnvironment} from 'config';

import GroupBy from './GroupBy';

jest.mock('config', () => ({
  isOptimizeCloudEnvironment: jest.fn().mockReturnValue(false),
}));

jest.mock('services', () => {
  const rest = jest.requireActual('services');

  return {
    ...rest,
    reportConfig: {
      ...rest.reportConfig,
      process: {
        group: [],
        distribution: [],
      },
    },
    updateReport: jest.fn(),
  };
});

const config = {
  type: 'process',
  variables: {variable: []},
  onChange: jest.fn(),
  report: {
    view: {},
    groupBy: {type: 'group'},
    distributedBy: {type: 'distribution'},
    definitions: [{id: 'definitionId'}],
  },
};

beforeEach(() => {
  reportConfig.process.group = [
    {
      key: 'none',
      matcher: jest.fn().mockReturnValue(false),
      visible: jest.fn().mockReturnValue(true),
      enabled: jest.fn().mockReturnValue(true),
      label: jest.fn().mockReturnValue('None'),
    },
    {
      key: 'group1',
      matcher: jest.fn().mockReturnValue(false),
      visible: jest.fn().mockReturnValue(true),
      enabled: jest.fn().mockReturnValue(true),
      label: jest.fn().mockReturnValue('Group 1'),
    },
    {
      key: 'group2',
      matcher: jest.fn().mockReturnValue(true),
      visible: jest.fn().mockReturnValue(true),
      enabled: jest.fn().mockReturnValue(true),
      label: jest.fn().mockReturnValue('Group 2'),
    },
    {
      key: 'variable',
      matcher: jest.fn().mockReturnValue(false),
      visible: jest.fn().mockReturnValue(true),
      enabled: jest.fn().mockReturnValue(true),
      label: jest.fn().mockReturnValue('Variable'),
    },
    {
      key: 'assignee',
      matcher: jest.fn().mockReturnValue(false),
      visible: jest.fn().mockReturnValue(true),
      enabled: jest.fn().mockReturnValue(true),
      label: jest.fn().mockReturnValue('assignee'),
    },
  ];
  reportConfig.process.distribution = [
    {
      key: 'none',
      matcher: jest.fn().mockReturnValue(false),
      visible: jest.fn().mockReturnValue(true),
      enabled: jest.fn().mockReturnValue(true),
      label: jest.fn().mockReturnValue('None'),
    },
    {
      key: 'distribution',
      matcher: jest.fn().mockReturnValue(true),
      visible: jest.fn().mockReturnValue(true),
      enabled: jest.fn().mockReturnValue(true),
      label: jest.fn().mockReturnValue('Sample Distribution'),
    },
  ];

  updateReport.mockClear();
});

it('should disable options which would create a wrong combination', () => {
  reportConfig.process.group[1].enabled.mockReturnValue(false);

  const node = shallow(<GroupBy {...config} />);

  expect(node.find(Select.Option).first()).toBeDisabled();
});

it('should disable the variable view submenu if there are no variables', () => {
  const node = shallow(<GroupBy {...config} />);

  expect(node.find(Select.Submenu)).toBeDisabled();
});

it('invoke configUpdate with the correct variable data', async () => {
  const spy = jest.fn();
  const node = shallow(
    <GroupBy
      {...config}
      variables={{variable: [{id: 'test', type: 'date', name: 'testName'}]}}
      onChange={spy}
    />
  );

  const selectedOption = {
    type: 'variable',
    value: {id: 'test', name: 'testName', type: 'date'},
  };

  updateReport.mockReturnValue({content: 'change'});

  node.find(Select).simulate('change', 'variable_testName');

  expect(updateReport.mock.calls[0][4].groupBy.value.$set).toEqual(selectedOption.value);
  expect(spy).toHaveBeenCalledWith({content: 'change'});
});

it('should use the distributedBy value when removing the groupBy', () => {
  const spy = jest.fn();
  const node = shallow(<GroupBy {...config} onChange={spy} />);

  node.find(Button).simulate('click');

  expect(updateReport.mock.calls[0][4].groupBy.$set).toEqual({type: 'distribution'});
});

it('should hide assignee option in cloud environment', async () => {
  isOptimizeCloudEnvironment.mockReturnValueOnce(true);
  const node = shallow(<GroupBy {...config} />);

  await runAllEffects();

  expect(node.find({value: 'assignee'})).not.toExist();
});

it('should pass null as a value to Select if groupBy is null', () => {
  const node = shallow(<GroupBy {...config} report={{...config.report, groupBy: null}} />);

  expect(node.find(Select).prop('value')).toBe(null);
});
