import React from 'react';
import PropTypes from 'prop-types';
import {isEqual} from 'lodash';

import {
  serializeInstancesMaps,
  deserializeInstancesMaps
} from 'modules/utils/selection/selection';
import {
  fetchWorkflowInstancesBySelection,
  fetchWorkflowInstancesByIds
} from 'modules/api/instances';
import {getSelectionById} from 'modules/utils/selection';
import withSharedState from 'modules/components/withSharedState';
import {DEFAULT_SELECTED_INSTANCES} from 'modules/constants';

import {createMapOfInstances, getInstancesIdsFromSelections} from './service';

const SelectionContext = React.createContext();

export const SelectionConsumer = SelectionContext.Consumer;

class BasicSelectionProvider extends React.Component {
  static propTypes = {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.node),
      PropTypes.node
    ]),
    getFilterQuery: PropTypes.func.isRequired,
    getStateLocally: PropTypes.func.isRequired,
    storeStateLocally: PropTypes.func.isRequired,
    filter: PropTypes.object
  };

  constructor(props) {
    super(props);

    // read data from local storage
    const {
      instancesInSelectionsCount,
      rollingSelectionIndex,
      selectionCount,
      selections
    } = props.getStateLocally();

    this.state = {
      instancesInSelectionsCount: instancesInSelectionsCount || 0,
      openSelection: null,
      rollingSelectionIndex: rollingSelectionIndex || 0,
      selectedInstances: DEFAULT_SELECTED_INSTANCES,
      selectionCount: selectionCount || 0,
      selections: deserializeInstancesMaps(selections) || []
    };
  }

  async componentDidMount() {
    if (this.state.selectionCount) {
      await this.refetchInstancesInSelections();
    }
  }

  componentDidUpdate(prevProps) {
    if (this.state.selectedInstances === DEFAULT_SELECTED_INSTANCES) {
      return;
    }

    // reset selected instances when filter changes
    if (!isEqual(prevProps.filter, this.props.filter)) {
      this.handleSelectedInstancesReset();
    }
  }

  /**
   * Gets the queries associatioed with a particular selection.
   * It combines the current filter query and the ids of instances in the selection
   */
  getSelectionQueries = () => {
    // add filter query to the query
    let query = this.props.getFilterQuery();

    if (!this.state.selectedInstances.all) {
      query.ids = [...this.state.selectedInstances.ids];
    } else {
      query.excludeIds = [...this.state.selectedInstances.excludeIds];
    }

    return [query];
  };

  /**
   * Gets the queries associatioed with a particular selection.
   * It combines the current filter query and the ids of instances in the selection
   * @param {string} selectionId: selectionId of the target selection.
   */
  getSelectionQueriesById = selectionId => {
    const {queries} = this.state.selections.find(
      selection => selection.selectionId === selectionId
    );

    return [...this.getSelectionQueries(), ...queries];
  };

  /**
   * Creates a new selection and adds it to the selection list
   */
  handleAddNewSelection = async () => {
    const queries = this.getSelectionQueries();

    // fetch new workflowInstances for the selection
    const {
      workflowInstances,
      totalCount
    } = await fetchWorkflowInstancesBySelection({queries});

    // update selection related data
    const rollingSelectionIndex = this.state.rollingSelectionIndex + 1;

    // add new selections to the list
    const selections = [
      {
        selectionId: rollingSelectionIndex,
        instancesMap: createMapOfInstances(workflowInstances),
        queries,
        totalCount
      },
      ...this.state.selections
    ];

    // add the count of the new instances in selection
    const instancesInSelectionsCount =
      this.state.instancesInSelectionsCount + totalCount;

    // increment the number of selections
    const selectionCount = this.state.selectionCount + 1;

    // set the updated data in the state
    this.setState({
      selections,
      rollingSelectionIndex,
      instancesInSelectionsCount,
      selectionCount,
      openSelection: rollingSelectionIndex,
      selectedInstances: DEFAULT_SELECTED_INSTANCES
    });

    // store the updated data in the local storage
    this.props.storeStateLocally({
      selections: serializeInstancesMaps(selections),
      rollingSelectionIndex,
      instancesInSelectionsCount,
      selectionCount
    });
  };

  /**
   * Adds the selected instances to a target selection
   * @param {string} selectionId: selectionId of the target selection.
   */
  handleAddToSelectionById = async selectionId => {
    const queries = this.getSelectionQueriesById(selectionId);

    // fetch new workflowInstances for the selection
    const {
      workflowInstances,
      totalCount
    } = await fetchWorkflowInstancesBySelection({queries});

    // get target selection from list of selections
    const {index: selectionIndex, totalCount: oldTotalCount} = getSelectionById(
      this.state.selections,
      selectionId
    );

    // update the target selection
    const selections = [
      ...this.state.selections.slice(0, selectionIndex),
      {
        ...this.state.selections[selectionIndex],
        instancesMap: createMapOfInstances(workflowInstances),
        queries,
        totalCount
      },
      ...this.state.selections.slice(selectionIndex + 1)
    ];

    const instancesInSelectionsCount =
      this.state.instancesInSelectionsCount - oldTotalCount + totalCount;

    // set the updated data in the state
    this.setState({
      selections,
      instancesInSelectionsCount,
      selectedInstances: DEFAULT_SELECTED_INSTANCES,
      openSelection: selectionId
    });

    // store the updated data in the local storage
    this.props.storeStateLocally({
      selections: serializeInstancesMaps(selections),
      instancesInSelectionsCount
    });
  };

  /**
   * Adds selected instances to the currently open selection
   */
  handleAddToOpenSelection = () => {
    this.handleAddToSelectionById(this.state.openSelection);
  };

  /**
   * Toggles a selection
   * @param {string} selectionId: selectionId of the selection to be toggled
   */
  handleToggleSelection = selectionId => {
    this.setState({
      openSelection:
        selectionId !== this.state.openSelection ? selectionId : null
    });
  };

  /**
   * Toggles a selection
   * @param {string} selectionId: selectionId of the selection to be deleted
   */
  handleDeleteSelectionById = async selectionId => {
    const {selections, instancesInSelectionsCount, selectionCount} = this.state;

    const selectionToRemove = getSelectionById(selections, selectionId);
    // remove the selection
    selections.splice(selectionToRemove.index, 1);

    this.setState(
      {
        selections,
        instancesInSelectionsCount:
          instancesInSelectionsCount - selectionToRemove.totalCount,
        selectionCount: selectionCount - 1 || 0
      },
      () => {
        this.props.storeStateLocally({
          selections: serializeInstancesMaps(selections),
          instancesInSelectionsCount: this.state.instancesInSelectionsCount,
          selectionCount: this.state.selectionCount
        });
      }
    );
  };

  /**
   * Re-fetches all workflow instances present in the selections
   */
  refetchInstancesInSelections = async () => {
    const IdsOfInstancesInSelections = getInstancesIdsFromSelections(
      this.state.selections
    );
    const {workflowInstances} = await fetchWorkflowInstancesByIds(
      IdsOfInstancesInSelections
    );
    const workflowInstancesMap = createMapOfInstances(workflowInstances);
    let selections = this.state.selections.map(
      ({instancesMap, ...selection}) => {
        const newMap = new Map();
        instancesMap.forEach(function(_, key) {
          const newValue = workflowInstancesMap.get(key);
          newMap.set(key, newValue);
        });
        return {instancesMap: newMap, ...selection};
      }
    );
    this.setState({selections});
  };

  /**
   * A setter for this.state.selection
   */
  handleSelectedInstancesUpdate = selectedInstances => {
    this.setState({selectedInstances});
  };

  /**
   * Resets this.state.selection to its default value
   */
  handleSelectedInstancesReset = () => {
    this.setState({selectedInstances: DEFAULT_SELECTED_INSTANCES});
  };

  render() {
    // values to be provided to the context consumers
    const contextValue = {
      ...this.state,
      onSelectedInstancesUpdate: this.handleSelectedInstancesUpdate,
      onSelectedInstancesReset: this.handleSelectedInstancesReset,
      onAddNewSelection: this.handleAddNewSelection,
      onAddToSelectionById: this.handleAddToSelectionById,
      onAddToOpenSelection: this.handleAddToOpenSelection,
      onToggleSelection: this.handleToggleSelection,
      onDeleteSelection: this.handleDeleteSelectionById
    };

    return (
      <SelectionContext.Provider value={contextValue}>
        {this.props.children}
      </SelectionContext.Provider>
    );
  }
}

export const SelectionProvider = withSharedState(BasicSelectionProvider);

/**
 * HOC that Wraps a component in a the selection consumer
 * @param {*} Component: Component to be wrapped
 */
export const withSelection = Component => {
  function WithSelection(props) {
    return (
      <SelectionConsumer>
        {contextValue => <Component {...props} {...contextValue} />}
      </SelectionConsumer>
    );
  }

  WithSelection.WrappedComponent = Component;

  WithSelection.displayName = `WithSelection(${Component.displayName ||
    Component.name ||
    'Component'})`;

  return WithSelection;
};
