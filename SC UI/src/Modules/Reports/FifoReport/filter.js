import React from 'react';
import CommonFilter from '../../../Shared/Filter';

class FifoReportFilter extends CommonFilter {
  renderFilter() {
    const { options = {} } = this.props;
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {this.renderFilterDate('Start Date', 'startDate')}
            {this.renderFilterDate('End Date', 'endDate')}
            {this.renderAutoComplete(
              'Project',
              options.projects || [],
              'tenantSchema',
              (o) => o.name,
              true
            )}
            {this.renderAutoComplete(
              'Product',
              options.products || [],
              'productName',
              (o) => o,
              true
            )}
            {this.renderAutoComplete(
              'Contractor',
              options.contractors || [],
              'contractorName',
              (o) => o,
              true
            )}
            {this.renderAutoComplete(
              'Performed By',
              options.performedBy || [],
              'performedBy',
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

export default FifoReportFilter;
