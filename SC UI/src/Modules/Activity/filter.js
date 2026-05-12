import React from 'react';
import CommonFilter from '../../Shared/Filter';

const ACTIONS = [
  'CREATED', 'UPDATED', 'DELETED', 'CANCELLED', 'SHORT_CLOSED', 'APPROVED', 'SPLIT',
];

const ENTITY_TYPES = [
  'INDENT', 'PURCHASE_ORDER', 'INWARD', 'OUTWARD',
  'LOST_DAMAGED', 'MACHINERY_ON_RENT', 'INVENTORY_TRANSFER',
];

class ActivityFilter extends CommonFilter {
  renderFilter() {
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {this.renderFilterDate('Start Date', 'startDate')}
            {this.renderFilterDate('End Date', 'endDate')}
            {this.renderAutoComplete(
              'Action',
              ACTIONS,
              'action',
              (o) => o,
              false
            )}
            {this.renderAutoComplete(
              'Entity Type',
              ENTITY_TYPES,
              'entityType',
              (o) => o,
              false
            )}
            {this.renderTextField('Entity ID', 'entityId')}
            {this.renderTextField('Performed By', 'performedBy')}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default ActivityFilter;
