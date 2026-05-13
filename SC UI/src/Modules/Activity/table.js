import React from 'react';
import CommonTable from '../../Shared/Table';
import { messages } from '../../messages';

const ACTION_COLORS = {
  CREATED:     { color: '#2e7d32', background: '#e8f5e9' },
  UPDATED:     { color: '#e65100', background: '#fff3e0' },
  DELETED:     { color: '#c62828', background: '#ffebee' },
  CANCELLED:   { color: '#6a1a6a', background: '#f9e5f9' },
  SHORT_CLOSED:{ color: '#0d47a1', background: '#e3f2fd' },
  APPROVED:    { color: '#1565c0', background: '#e3f2fd' },
  SPLIT:       { color: '#4e342e', background: '#efebe9' },
};

class ActivityTable extends CommonTable {
  renderCell(key, row) {
    if (key === 'action') {
      const style = ACTION_COLORS[row.action] || {};
      return (
        <td data-label="Action">
          <span style={{
            ...style,
            padding: '2px 8px',
            borderRadius: '4px',
            fontWeight: 600,
            fontSize: '12px',
          }}>
            {row.action}
          </span>
        </td>
      );
    }
    if (key === 'activityTime') {
      const formatted = row.activityTime
        ? new Date(row.activityTime).toLocaleString()
        : '';
      return <td data-label="Time" style={{ whiteSpace: 'nowrap' }}>{formatted}</td>;
    }
    if (key === 'description') {
      return (
        <td data-label="Description" style={{ maxWidth: '320px', wordBreak: 'break-word' }}>
          {row.description}
        </td>
      );
    }
    if (key === 'tenantSchema') {
      return <td data-label="Project" style={{ whiteSpace: 'nowrap' }}>{row.tenantSchema}</td>;
    }
    return super.renderCell(key, row);
  }

  renderBody() {
    const rows = this.state.rows || [];
    const keys = this.state.keys;
    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={keys.length}>{messages.common.noRecords}</td>
        </tr>
      );
    }
    return rows.map((row, index) => (
      <tr key={index} className={index === rows.length - 1 ? 'row last' : 'row'}>
        {keys.map((key) => this.renderCell(key, row))}
      </tr>
    ));
  }
}

export default ActivityTable;
