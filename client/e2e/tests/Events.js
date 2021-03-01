/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {cleanEventProcesses} from '../setup';
import config from '../config';
import * as u from '../utils';

import * as e from './Events.elements.js';
import * as Homepage from './Homepage.elements';

fixture('Events Processes').page(config.endpoint).beforeEach(u.login).after(cleanEventProcesses);

test('create a process from scratch', async (t) => {
  await t.click(e.navItem);
  await t.click(e.createDropdown);
  await t.click(e.dropdownOption('Model a Process'));
  await t.typeText(e.nameEditField, 'Invoice Process', {replace: true});
  await t.click(e.firstEl);
  await t.click(e.activityTask);
  await t.click(e.saveButton);
  await t.expect(e.processName.textContent).eql('Invoice Process');
});

test('add sources, map and publish a process', async (t) => {
  // Creation
  await t.resizeWindow(1100, 800);
  await t.click(e.navItem);
  await t.click(e.createDropdown);
  await t.setFilesToUpload(e.fileInput, './resources/eventsProcess.bpmn');
  await t.click(e.entity('Event Invoice process'));
  await t.click(e.editButton);

  await t.typeText(e.nameEditField, 'Event Invoice process', {replace: true});

  await t.takeScreenshot('event-based-processes/editMode.png');

  // adding sources

  await t.click(e.addSource);

  await t.click(e.optionsButton(e.processTypeahead));
  await t.typeText(e.typeaheadInput(e.processTypeahead), 'Invoice', {replace: true});
  await t.click(e.typeaheadOption(e.processTypeahead, 'Invoice Receipt'));
  await t.expect(e.optionsButton(e.variableTypeahead).hasAttribute('disabled')).notOk();
  await t.click(e.optionsButton(e.variableTypeahead));
  await t.click(e.typeaheadOption(e.variableTypeahead, 'longVar'));
  await t.click(e.startAndEndEvents);

  await t.takeElementScreenshot(e.modalContainer, 'event-based-processes/sourceModal.png');

  await t.click(e.primaryModalButton);

  await t.click(e.addSource);
  await t.click(e.externalEvents);
  await t.click(e.externalEventgroup(2));
  await t.click(e.externalEventgroup(3));
  await t.takeElementScreenshot(e.modalContainer, 'event-based-processes/externalEvents.png');
  await t.click(e.primaryModalButton);

  await t.click(e.addSource);
  await t.click(e.externalEvents);
  await t.click(e.externalEventgroup(0));
  await t.click(e.primaryModalButton);

  await t.takeElementScreenshot(e.eventsTable, 'event-based-processes/eventsTable.png');

  // Mapping

  await t.click(e.startNode);
  await t.click(e.startEvent);

  await t.click(e.activity);
  await t.click(e.bankEnd);
  await t.click(e.bankStart);

  await t.click(e.endNode);
  await t.click(e.endEvent);

  await t.click(e.saveButton);

  await t.click(e.zoomButton);
  await t.click(e.zoomButton);

  await t.takeScreenshot('event-based-processes/processView.png');

  // publishing

  await t.click(e.publishButton);

  await t.takeElementScreenshot(e.modalContainer, 'event-based-processes/publishModal.png');

  await t.click(e.permissionButton);

  await t.click(e.optionsButton(e.usersTypeahead));
  // Using exact userId in the search to eliminate potential race condition on the identity being available in
  // the search cache. Id usage will always work regardless of the search cache state.
  await t.typeText(e.typeaheadInput(e.usersTypeahead), 'john', {replace: true});
  await t.click(e.typeaheadOption(e.usersTypeahead, 'john'));

  await t.takeElementScreenshot(e.modalContainer.nth(1), 'event-based-processes/usersModal.png');

  await t.click(e.primaryModalButton.nth(1));
  await t.click(e.primaryModalButton);
  await t.expect(e.notification.exists).ok({timeout: 5000});
  await t.click(e.notificationCloseButton(e.notification));

  // Listing
  await t.click(e.navItem);
  await t.hover(e.entity('Event Invoice process'));
  await t.click(Homepage.contextMenu(e.invoiceEventProcess));
  await t.takeScreenshot('event-based-processes/processList.png');

  // Edit Access
  await t.click(e.editAccess(e.invoiceEventProcess));
  await t.takeElementScreenshot(e.modalContainer.nth(0), 'event-based-processes/editAccess.png');
});

test('auto generate a process', async (t) => {
  await t.click(e.navItem);
  await t.click(e.createDropdown);
  await t.click(e.dropdownOption('Autogenerate'));
  await t.click(e.buttonWithText('Add Event Source'));

  await t.click(e.optionsButton(e.processTypeahead));
  await t.typeText(e.typeaheadInput(e.processTypeahead), 'Invoice', {replace: true});
  await t.click(e.typeaheadOption(e.processTypeahead, 'Invoice Receipt'));
  await t.click(e.businessKey);
  await t.click(e.primaryModalButton.nth(1));

  await t.click(e.buttonWithText('Add Event Source'));
  await t.click(e.externalEvents);
  await t.click(e.primaryModalButton.nth(1));

  await t.takeElementScreenshot(e.modalContainer, 'event-based-processes/auto-generation.png');

  await t.click(e.buttonWithText('Generate'));

  await t.expect(e.diagram.visible).ok();
});

test('delete multiple external events', async (t) => {
  await t.click(e.navItem);
  await t.click(e.externalEventsTab);

  await t.resizeWindow(1100, 800);

  await t.takeScreenshot('event-based-processes/external-events.png', {fullPage: true});

  await t.click(e.eventCheckbox(0));
  await t.click(e.eventCheckbox(3));
  await t.click(e.selectionDropdown);
  await t.hover(e.dropdownOption('Delete'));

  await t.takeScreenshot('event-based-processes/deleting-events.png', {fullPage: true});

  await t.click(e.dropdownOption('Delete'));
  await t.click(e.confirmModalButton);
});
