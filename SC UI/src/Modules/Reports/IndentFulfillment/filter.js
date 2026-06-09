import React from 'react';
import CommonFilter from '../../../Shared/Filter';

const INDENT_STATUSES = ['NEW', 'APPROVED', 'PO CREATED', 'PO PARTIAL', 'INWARD PARTIAL', 'PO COMPLETED', 'CLOSED', 'CANCELLED', 'REJECTED'];
const LINE_STATUSES   = ['NEW', 'PO CREATED', 'INWARD PARTIAL', 'INWARD COMPLETE', 'SHORT CLOSED', 'CANCELLED'];

class IndentFulfillmentFilter extends CommonFilter {

  renderChips(label, fieldname, options) {
    const selected = this.filterData[fieldname] || '';
    return (
      <div className="filter-item">
        <label style={{ fontSize: 12, color: '#666', display: 'block', marginBottom: 4 }}>{label}</label>
        <div style={{ display: 'flex', gap: 5, flexWrap: 'wrap' }}>
          {options.map((s) => {
            const active = selected === s;
            return (
              <span
                key={s}
                onClick={() => {
                  this.filterData[fieldname] = active ? '' : s;
                  this.setState({});
                }}
                style={{
                  padding: '3px 10px',
                  borderRadius: 12,
                  fontSize: 11,
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
            {this.renderChips('Indent Status', 'indentStatus', INDENT_STATUSES)}
            {this.renderChips('Line Status', 'lineItemStatus', LINE_STATUSES)}
            {this.renderFilterDate('Indent Date From', 'startDate')}
            {this.renderFilterDate('Indent Date To', 'endDate')}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default IndentFulfillmentFilter;
