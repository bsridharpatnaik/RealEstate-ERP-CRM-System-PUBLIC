import React from 'react';
import CommonFilter from '../../Shared/Filter';

class DeadStockFilter extends CommonFilter {
  renderFilter() {
    const { options = {} } = this.props;
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {this.renderAutoComplete(
              'Project',
              options.projects || [],
              'tenantSchema',
              (o) => o.name,
              true
            )}
            {this.renderAutoComplete(
              'Category',
              options.categories || [],
              'category',
              (o) => o,
              true
            )}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default DeadStockFilter;
