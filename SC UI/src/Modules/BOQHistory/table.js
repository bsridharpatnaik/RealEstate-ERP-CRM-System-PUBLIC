import React from 'react';
import CommonTable from '../../Shared/Table';
import { messages } from '../../messages';

const CHANGE_TYPE_COLORS = {
  Added:   { color: '#2e7d32', background: '#e8f5e9' },
  Updated: { color: '#e65100', background: '#fff3e0' },
  Deleted: { color: '#c62828', background: '#ffebee' },
};

class Table extends CommonTable {
  renderCell(key, row) {
    if (key === 'changeType') {
      const style = CHANGE_TYPE_COLORS[row.changeType] || {};
      return (
        <td data-label="Change Type">
          <span style={{
            ...style,
            padding: '2px 8px',
            borderRadius: '4px',
            fontWeight: 600,
            fontSize: '12px',
          }}>
            {row.changeType}
          </span>
        </td>
      );
    }
    if (key === 'changeDateTime') {
      const formatted = row.changeDateTime
        ? new Date(row.changeDateTime).toLocaleString()
        : '';
      return <td data-label="Change Date Time" style={{ whiteSpace: 'nowrap' }}>{formatted}</td>;
    }
    if (key === 'oldQuantity' || key === 'newQuantity') {
      const val = row[key];
      return <td data-label={key}>{val != null ? val : '—'}</td>;
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

export default Table;
