import React from 'react';
import CommonFilter from '../../Shared/Filter';

const CHANGE_TYPES = ['Added', 'Updated', 'Deleted'];

class BOQHistoryFilter extends CommonFilter {
  renderFilter() {
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {this.renderFilterDate('Start Date', 'startDate')}
            {this.renderFilterDate('End Date', 'endDate')}
            {this.renderTextField('Building Type', 'buildingType')}
            {this.renderTextField('Building Unit', 'buildingUnit')}
            {this.renderTextField('Inventory', 'inventory')}
            {this.renderAutoComplete(
              'Change Type',
              CHANGE_TYPES,
              'changeType',
              (o) => o,
              false
            )}
            {this.renderTextField('Changed By', 'changedBy')}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default BOQHistoryFilter;
