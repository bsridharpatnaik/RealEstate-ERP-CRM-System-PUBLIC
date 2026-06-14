import React from 'react';
import CommonFilter from '../../../Shared/Filter';

const sectionLabel = (text) => (
  <div style={{
    fontSize: 11, fontWeight: 700, textTransform: 'uppercase',
    letterSpacing: 0.8, color: '#a0aec0',
    borderBottom: '1px solid #edf2f7',
    paddingBottom: 6, marginBottom: 4, marginTop: 4,
  }}>
    {text}
  </div>
);

class LowStockFilter extends CommonFilter {

  renderFilter() {
    const { options = {} } = this.props;
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content" style={{ padding: '16px 20px' }}>

            {sectionLabel('Filter By')}
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
