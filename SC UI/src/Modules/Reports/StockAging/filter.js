import React from 'react';
import CommonFilter from '../../../Shared/Filter';
import Switch from '@material-ui/core/Switch';

const AGING_BUCKETS = ['0-30', '31-60', '61-90', '90+'];

class StockAgingFilter extends CommonFilter {

  renderToggleBuckets() {
    const selected = this.filterData['agingBucket'] || [];
    const arr = Array.isArray(selected) ? selected : [selected];

    return (
      <div className="filter-item">
        <label style={{ fontSize: 12, color: '#666', display: 'block', marginBottom: 4 }}>Aging Bucket</label>
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          {AGING_BUCKETS.map((b) => {
            const active = arr.includes(b);
            return (
              <span
                key={b}
                onClick={() => {
                  const current = Array.isArray(this.filterData['agingBucket'])
                    ? this.filterData['agingBucket']
                    : (this.filterData['agingBucket'] ? [this.filterData['agingBucket']] : []);
                  this.filterData['agingBucket'] = active
                    ? current.filter((x) => x !== b)
                    : [...current, b];
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
                {b} days
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
              'Category',
              options.categories || [],
              'category',
              (o) => o,
              true
            )}
            {this.renderToggleBuckets()}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default StockAgingFilter;
