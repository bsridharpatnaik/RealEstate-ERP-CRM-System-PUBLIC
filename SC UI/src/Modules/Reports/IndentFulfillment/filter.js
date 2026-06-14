import React from 'react';
import CommonFilter from '../../../Shared/Filter';

// Mirrors the card badge colors so chips feel familiar
const INDENT_STATUS_OPTIONS = [
  { value: 'NEW',            label: 'New',            color: '#7f8c8d', bg: '#f2f3f4', border: '#d5d8dc' },
  { value: 'APPROVED',       label: 'Approved',       color: '#8e44ad', bg: '#f5eef8', border: '#d7bde2' },
  { value: 'PO CREATED',     label: 'PO Created',     color: '#2980b9', bg: '#ebf5fb', border: '#aed6f1' },
  { value: 'PO PARTIAL',     label: 'PO Partial',     color: '#f39c12', bg: '#fef9e7', border: '#fad7a0' },
  { value: 'INWARD PARTIAL', label: 'Inward Partial', color: '#f39c12', bg: '#fef9e7', border: '#fad7a0' },
  { value: 'PO COMPLETED',   label: 'PO Completed',   color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
  { value: 'CLOSED',         label: 'Closed',         color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
  { value: 'SHORT CLOSED',   label: 'Short Closed',   color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
  { value: 'CANCELLED',      label: 'Cancelled',      color: '#c0392b', bg: '#fdedec', border: '#f1948a' },
  { value: 'REJECTED',       label: 'Rejected',       color: '#c0392b', bg: '#fdedec', border: '#f1948a' },
];

const LINE_STATUS_OPTIONS = [
  { value: 'NEW',              label: 'New',              color: '#7f8c8d', bg: '#f2f3f4', border: '#d5d8dc' },
  { value: 'PO CREATED',      label: 'PO Created',       color: '#2980b9', bg: '#ebf5fb', border: '#aed6f1' },
  { value: 'INWARD PARTIAL',  label: 'Inward Partial',   color: '#f39c12', bg: '#fef9e7', border: '#fad7a0' },
  { value: 'INWARD COMPLETE', label: 'Inward Complete',  color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
  { value: 'SHORT CLOSED',    label: 'Short Closed',     color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
  { value: 'CANCELLED',       label: 'Cancelled',        color: '#c0392b', bg: '#fdedec', border: '#f1948a' },
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

class IndentFulfillmentFilter extends CommonFilter {

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
                padding: '4px 12px',
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
                {this.renderAutoComplete('Project', options.projects || [], 'project', (o) => o, true)}
              </div>
              <div>
                {this.renderAutoComplete('Product', options.products || [], 'productName', (o) => o, true)}
              </div>
            </div>

            {/* Row 2: Date range */}
            {sectionLabel('Date Range')}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: -12, marginBottom: 4 }}>
              <div>{this.renderFilterDate('Indent Date From', 'startDate')}</div>
              <div>{this.renderFilterDate('Indent Date To', 'endDate')}</div>
            </div>

            {/* Indent Status chips */}
            {sectionLabel('Indent Status')}
            <div style={{ marginBottom: 16 }}>
              {this.renderColorChips('indentStatus', INDENT_STATUS_OPTIONS)}
            </div>

            {/* Line Status chips */}
            {sectionLabel('Line Item Status')}
            <div style={{ marginBottom: 8 }}>
              {this.renderColorChips('lineItemStatus', LINE_STATUS_OPTIONS)}
            </div>

          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default IndentFulfillmentFilter;
