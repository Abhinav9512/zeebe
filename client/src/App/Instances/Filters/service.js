import {isValid, addDays, startOfDay, addMinutes, format} from 'date-fns';

export const FIELDS = {
  errorMessage: {name: 'errorMessage', placeholder: 'Error Message'},
  ids: {name: 'ids', placeholder: 'Instance Id(s) separated by space or comma'},
  workflowName: {name: 'workflowName', placeholder: 'Workflow'},
  workflowVersion: {name: 'workflowVersion', placeholder: 'Workflow Version'},
  flowNode: {name: 'flowNode', placeholder: 'Flow Node'},
  startDate: {name: 'startDate', placeholder: 'Start Date'},
  endDate: {name: 'endDate', placeholder: 'End Date'}
};

export function parseWorkflowNames(workflows) {
  return workflows.map(item => ({
    value: item.bpmnProcessId,
    label: item.name || item.bpmnProcessId
  }));
}

export function parseWorkflowVersions(versions = []) {
  return versions.map(item => ({
    value: item.id,
    label: `Version ${item.version}`
  }));
}

export function addAllVersionsOption(options = []) {
  options.push({value: 'all', label: 'All versions'});
  return options;
}

const parseDate = (value, name) => {
  let date = new Date(value);
  const isValidDate = isValid(date);
  let dateAfter, dateBefore;
  // enforce no comma in the timezone
  const formatWithTimezone = 'YYYY-MM-DDTHH:mm:ss.SSSZZ';

  if (!isValidDate) {
    return null;
  }

  // temporary condition to check for presence of time in user input
  // as we can't decide based on a string
  const hasTime = value.indexOf(':') !== -1;

  dateAfter = hasTime ? date : startOfDay(date);
  dateBefore = hasTime ? addMinutes(date, 1) : addDays(date, 1);

  return {
    [`${name}After`]: format(dateAfter, formatWithTimezone),
    [`${name}Before`]: format(dateBefore, formatWithTimezone)
  };
};

export const fieldParser = {
  errorMessage: value => (value.length === 0 ? null : value),
  ids: value => value.split(/[ ,]+/).filter(Boolean),
  startDate: value => {
    return parseDate(value, 'startDate');
  },
  endDate: value => {
    return parseDate(value, 'endDate');
  }
};
