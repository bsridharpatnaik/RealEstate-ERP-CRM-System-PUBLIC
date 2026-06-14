import React from 'react';
import CommonFilter from '../../../Shared/Filter';

class SupplierPerformanceFilter extends CommonFilter {
  renderFilter() {
    const { options = {} } = this.props;
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div>{this.renderFilterDate('Start Date', 'startDate')}</div>
              <div>{this.renderFilterDate('End Date', 'endDate')}</div>
              <div>{this.renderTextField('Supplier Name', 'supplierName', 100)}</div>
              <div>{this.renderAutoComplete('Category', options.categories || [], 'categoryName', (o) => o, false)}</div>
            </div>
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default SupplierPerformanceFilter;
