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

class FifoReportFilter extends CommonFilter {
  renderFilter() {
    const { options = {} } = this.props;
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content" style={{ padding: '16px 20px' }}>

            {sectionLabel('Date Range')}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: -16, marginBottom: 4 }}>
              <div>{this.renderFilterDate('Start Date', 'startDate')}</div>
              <div>{this.renderFilterDate('End Date', 'endDate')}</div>
            </div>

            {sectionLabel('Search')}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 8 }}>
              <div>
                {this.renderAutoComplete('Project', options.projects || [], 'tenantSchema', (o) => o.name, true)}
              </div>
              <div>
                {this.renderAutoComplete('Product', options.products || [], 'productName', (o) => o, true)}
              </div>
              <div>
                {this.renderAutoComplete('Contractor', options.contractors || [], 'contractorName', (o) => o, true)}
              </div>
              <div>
                {this.renderAutoComplete('Performed By', options.performedBy || [], 'performedBy', (o) => o, true)}
              </div>
            </div>

          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default FifoReportFilter;
