import React from 'react';
import CommonFilter from '../../../Shared/Filter';

const AGING_BUCKETS = ['0-30', '31-60', '61-90', '90+'];

const sectionLabel = (text) => (
  <div style={{
    fontSize: 11, fontWeight: 700, textTransform: 'uppercase',
    letterSpacing: 0.8, color: '#a0aec0',
    borderBottom: '1px solid #edf2f7',
    paddingBottom: 6, marginBottom: 8, marginTop: 4,
  }}>
    {text}
  </div>
);

class StockAgingFilter extends CommonFilter {
  labelsOutside = true;

  renderToggleBuckets() {
    const selected = this.filterData['agingBucket'] || [];
    const arr = Array.isArray(selected) ? selected : [selected];

    return (
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
                padding: '5px 14px',
                borderRadius: 20,
                fontSize: 12,
                fontWeight: 600,
                cursor: 'pointer',
                border: '1.5px solid',
                borderColor: active ? '#323c47' : '#d0d7de',
                background: active ? '#323c47' : '#f5f5f5',
                color: active ? '#fff' : '#555',
                userSelect: 'none',
                transition: 'all 0.15s',
              }}
            >
              {b} days
            </span>
          );
        })}
      </div>
    );
  }

  renderFilter() {
    const { options = {} } = this.props;
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content" style={{ padding: '16px 20px' }}>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 12 }}>
              <div>
                {this.renderAutoComplete('Project', options.projects || [], 'tenantSchema', (o) => o.name, true)}
              </div>
              <div>
                {this.renderAutoComplete('Category', options.categories || [], 'category', (o) => o, true)}
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                {this.renderAutoComplete('Product', options.products || [], 'productName', (o) => o, true)}
              </div>
            </div>

            {sectionLabel('Aging Bucket')}
            <div style={{ marginBottom: 8 }}>
              {this.renderToggleBuckets()}
            </div>

          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default StockAgingFilter;
