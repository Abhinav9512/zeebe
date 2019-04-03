/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {getTargetLineOptions} from './createTargetLineOptions';

it('should should return correct option for line chart with target value', () => {
  const options = getTargetLineOptions('testColor', true, true, true);
  expect(options).toEqual({
    normalLineOptions: {
      backgroundColor: 'transparent',
      borderColor: 'testColor',
      borderWidth: 2,
      legendColor: 'testColor',
      renderArea: 'top'
    },
    targetOptions: {
      backgroundColor: 'transparent',
      borderColor: 'testColor',
      borderWidth: 2,
      legendColor: 'testColor',
      pointBorderColor: '#A62A31',
      renderArea: 'bottom'
    }
  });
});
