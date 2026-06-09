import React from 'react';
import CommonFilter from '../../../Shared/Filter';

const PO_STATUSES = ['OPEN', 'COMPLETED', 'SHORT_CLOSED', 'CANCELLED'];

class PoReconFilter extends CommonFilter {

  renderStatusChips() {
    const selected = this.filterData['poStatus'] || '';
    return (
      <div className="filter-item">
        <label style={{ fontSize: 12, color: '#666', display: 'block', marginBottom: 4 }}>PO Status</label>
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          {PO_STATUSES.map((s) => {
            const active = selected === s;
            return (
              <span
                key={s}
                onClick={() => {
                  this.filterData['poStatus'] = active ? '' : s;
                  this.setState({});
                }}
                style={{
                  padding: '4px 12px',
                  borderRadius: 12,
                  fontSize: 12,
                  fontWeight: 600,
                  cursor: 'pointer',
                  border: '1px solid #ccc',
                  background: active ? '#323c47' : '#f5f5f5',
                  color: active ? '#fff' : '#555',
                  userSelect: 'none',
                }}
              >
                {s}
              </span>
            );
          })}
        </div>
      </div>
    );
  }

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
              'project',
              (o) => o,
              false
            )}
            {this.renderTextField('Product Name', 'productName')}
            {this.renderStatusChips()}
            {this.renderFilterDate('PO Date From', 'startDate')}
            {this.renderFilterDate('PO Date To', 'endDate')}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default PoReconFilter;
