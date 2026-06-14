import React from 'react';
import CommonFilter from '../../../Shared/Filter';

class LowStockFilter extends CommonFilter {
  labelsOutside = true;

  renderFilter() {
    const { options = {} } = this.props;
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content" style={{ padding: '16px 20px' }}>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 8 }}>
              <div>
                {this.renderAutoComplete('Project', options.projects || [], 'tenantSchema', (o) => o.name, true)}
              </div>
              <div>
                {this.renderAutoComplete('Category', options.categories || [], 'category', (o) => o, true)}
              </div>
            </div>

          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default LowStockFilter;
