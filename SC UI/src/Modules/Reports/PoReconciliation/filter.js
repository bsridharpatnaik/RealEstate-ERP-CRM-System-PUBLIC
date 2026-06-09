import React from 'react';
import CommonFilter from '../../../Shared/Filter';

const PO_STATUS_OPTIONS = [
  { value: 'OPEN',         label: 'Open',         color: '#2980b9', bg: '#ebf5fb', border: '#aed6f1' },
  { value: 'COMPLETED',    label: 'Completed',    color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
  { value: 'SHORT_CLOSED', label: 'Short Closed', color: '#d35400', bg: '#fdf2e9', border: '#f0b27a' },
  { value: 'CANCELLED',    label: 'Cancelled',    color: '#c0392b', bg: '#fdedec', border: '#f1948a' },
];

const RECON_STATUS_OPTIONS = [
  { value: 'NOT_STARTED', label: 'Not Started', color: '#95a5a6', bg: '#f2f3f4', border: '#d5d8dc' },
  { value: 'PARTIAL',     label: 'Partial',     color: '#f39c12', bg: '#fef9e7', border: '#fad7a0' },
  { value: 'COMPLETE',    label: 'Complete',    color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
];

const sectionLabel = (text) => (
  <div style={{
    fontSize: 11, fontWeight: 700, textTransform: 'uppercase',
    letterSpacing: 0.8, color: '#a0aec0',
    borderBottom: '1px solid #edf2f7',
    paddingBottom: 6, marginBottom: 10, marginTop: 4,
  }}>
    {text}
  </div>
);

class PoReconFilter extends CommonFilter {

  renderColorChips(fieldname, options) {
    const selected = this.filterData[fieldname] || '';
    return (
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {options.map((opt) => {
          const active = selected === opt.value;
          return (
            <span
              key={opt.value}
              onClick={() => {
                this.filterData[fieldname] = active ? '' : opt.value;
                this.setState({});
              }}
              style={{
                padding: '5px 14px',
                borderRadius: 20,
                fontSize: 12,
                fontWeight: 600,
                cursor: 'pointer',
                userSelect: 'none',
                transition: 'all 0.15s',
                background: active ? opt.color : opt.bg,
                color: active ? '#fff' : opt.color,
                border: `1.5px solid ${active ? opt.color : opt.border}`,
                boxShadow: active ? `0 2px 6px ${opt.color}40` : 'none',
              }}
            >
              {opt.label}
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

            {/* Row 1: Project + Product */}
            {sectionLabel('Search')}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 16 }}>
              <div>
                {this.renderAutoComplete('Project', options.projects || [], 'project', (o) => o, false)}
              </div>
              <div>
                {this.renderTextField('Product Name', 'productName')}
              </div>
            </div>

            {/* Row 2: Date range */}
            {sectionLabel('PO Date Range')}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 16 }}>
              <div>{this.renderFilterDate('From', 'startDate')}</div>
              <div>{this.renderFilterDate('To', 'endDate')}</div>
            </div>

            {/* PO Status chips */}
            {sectionLabel('PO Status')}
            <div style={{ marginBottom: 16 }}>
              {this.renderColorChips('poStatus', PO_STATUS_OPTIONS)}
            </div>

            {/* Reconciliation Status chips */}
            {sectionLabel('Reconciliation Status')}
            <div style={{ marginBottom: 8 }}>
              {this.renderColorChips('reconciliationStatus', RECON_STATUS_OPTIONS)}
            </div>

          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default PoReconFilter;
