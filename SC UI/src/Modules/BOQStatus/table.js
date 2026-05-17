//react
import React from "react";
//third party
//style
import "./style.scss";
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";
import IconButton from "@material-ui/core/IconButton";
import Checkbox from "@material-ui/core/Checkbox";
import EditIcon from "@material-ui/icons/Edit";
import DeleteIcon from "@material-ui/icons/Delete";
import KeyboardArrowDownIcon from "@material-ui/icons/KeyboardArrowDown";
import KeyboardArrowUpIcon from "@material-ui/icons/KeyboardArrowUp";

class Table extends CommonTable {

  state = {
    ...(super.state || {}),
    expandedRowId: null,
  };

  // status = (outward - boq) / boq * 100
  // consumed% = status + 100
  // on-track: consumed < 80%  → status < -20
  // at-risk:  consumed 80-100% → -20 <= status <= 0
  // exceeded: consumed > 100%  → status > 0
  getStatusClass(status) {
    if (status > 0)    return "exceeded";
    if (status >= -20) return "at-risk";
    return "on-track";
  }

  toggleExpand = (id) => {
    this.setState(prev => ({
      expandedRowId: prev.expandedRowId === id ? null : id,
    }));
  };

  getStatusBucketColor(statusBucket) {
    if (!statusBucket) return '#888';
    const s = statusBucket.toLowerCase();
    if (s.includes('on track')) return '#2e7d32';
    if (s.includes('at risk'))  return '#e65100';
    if (s.includes('exceeded')) return '#c62828';
    return '#555';
  }

  getStatusBucketBg(statusBucket) {
    if (!statusBucket) return '#f5f5f5';
    const s = statusBucket.toLowerCase();
    if (s.includes('on track')) return '#e8f5e9';
    if (s.includes('at risk'))  return '#fff3e0';
    if (s.includes('exceeded')) return '#ffebee';
    return '#f5f5f5';
  }

  renderCell(key, row, headerIndex) {
    if (key === "id") {
      const expanded = this.state.expandedRowId === row.id;
      return (
        <td data-label={messages.common.id} style={{ whiteSpace: 'nowrap' }}>
          <IconButton
            size="small"
            onClick={() => this.toggleExpand(row.id)}
            title={expanded ? 'Collapse details' : 'Expand details'}
          >
            {expanded ? <KeyboardArrowUpIcon fontSize="small" /> : <KeyboardArrowDownIcon fontSize="small" />}
          </IconButton>
          <span style={{ marginLeft: 4, verticalAlign: 'middle' }}>{row["id"]}</span>
        </td>
      );
    } else if (key === "status") {
      const statusVal = row[key] !== null && row[key] !== undefined ? row[key] : 0;
      // consumed% = statusVal + 100, capped for bar width
      const consumed = statusVal + 100;
      const barWidth = Math.min(Math.max(consumed, 0), 100);
      const cls = this.getStatusClass(statusVal);
      return (
        <td data-label={messages.common.boqStatus}>
          <div className="boq-progress-cell">
            <div className="boq-progress-bar">
              <div
                className={`boq-progress-fill ${cls}`}
                style={{ width: `${barWidth}%` }}
              />
            </div>
            <span className={`boq-progress-label ${cls}`}>
              {Math.max(consumed, 0).toFixed(0)}%
            </span>
          </div>
        </td>
      );
    } else if (key === 'boqQuantity' || key === 'outwardQuantity') {
      const val = row[key];
      const display = val !== null && val !== undefined ? Number(val).toFixed(2) : '—';
      return <td data-label={key}>{display}</td>;
    } else {
      return super.renderCell(key, row, headerIndex);
    }
  }

  renderExpansionRow(row, colSpan) {
    const details = row.boqDetails || [];
    const canEdit = this.props.canEditBOQ;
    return (
      <tr key={`expand-${row.id}`} className="boq-expansion-row">
        <td colSpan={colSpan} style={{ padding: 0, borderTop: '1px solid #e0e0e0' }}>
          <div style={{ background: '#f9f9f9', padding: '12px 24px' }}>
            {details.length === 0 ? (
              <span style={{ color: '#888', fontSize: 13 }}>No work-area details available.</span>
            ) : (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid #ddd' }}>
                    <th style={{ textAlign: 'left', padding: '6px 10px', fontWeight: 600, color: '#555' }}>Work Area</th>
                    <th style={{ textAlign: 'right', padding: '6px 10px', fontWeight: 600, color: '#555' }}>BOQ Qty</th>
                    <th style={{ textAlign: 'right', padding: '6px 10px', fontWeight: 600, color: '#555' }}>Consumed Qty</th>
                    <th style={{ textAlign: 'center', padding: '6px 10px', fontWeight: 600, color: '#555' }}>Status</th>
                    {canEdit && <th style={{ width: 80 }}></th>}
                  </tr>
                </thead>
                <tbody>
                  {details.map((d, i) => (
                    <tr key={i} style={{ borderBottom: '1px solid #eee' }}>
                      <td style={{ padding: '5px 10px' }}>{d.finalLocation || '—'}</td>
                      <td style={{ padding: '5px 10px', textAlign: 'right' }}>
                        {d.boqQuantity != null ? Number(d.boqQuantity).toFixed(2) : '—'}
                      </td>
                      <td style={{ padding: '5px 10px', textAlign: 'right' }}>
                        {d.outwardQuantity != null ? Number(d.outwardQuantity).toFixed(2) : '—'}
                      </td>
                      <td style={{ padding: '5px 10px', textAlign: 'center' }}>
                        {d.statusBucket ? (
                          <span style={{
                            display: 'inline-block',
                            padding: '2px 10px',
                            borderRadius: 12,
                            fontSize: 12,
                            fontWeight: 600,
                            color: this.getStatusBucketColor(d.statusBucket),
                            background: this.getStatusBucketBg(d.statusBucket),
                          }}>
                            {d.statusBucket}
                          </span>
                        ) : '—'}
                      </td>
                      {canEdit && (
                        <td style={{ padding: '3px 6px', textAlign: 'center', whiteSpace: 'nowrap' }}>
                          <IconButton
                            size="small"
                            title="Edit this work area entry"
                            onClick={() => this.props.onEditDetail(row, d)}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <IconButton
                            size="small"
                            title="Delete this work area entry"
                            onClick={() => this.props.onDeleteDetail(d)}
                          >
                            <DeleteIcon fontSize="small" style={{ color: '#c62828' }} />
                          </IconButton>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </td>
      </tr>
    );
  }

  renderHeader() {
    const canEdit = this.props.canEditBOQ;
    const rows = this.state.rows || [];
    const selectedRowIds = this.props.selectedRowIds || [];
    const allSelected = rows.length > 0 && rows.every(r => selectedRowIds.includes(r.id));
    const someSelected = rows.some(r => selectedRowIds.includes(r.id));
    return (
      <tr>
        {canEdit && (
          <th style={{ width: 40 }}>
            <Checkbox
              size="small"
              checked={allSelected}
              indeterminate={someSelected && !allSelected}
              onChange={(e) => this.props.onSelectAll(e.target.checked)}
            />
          </th>
        )}
        {(this.state.headers || []).map((header, index) => (
          <th key={index} onClick={() => this.sort(index)} className="sortable">
            <span>{header}</span>
          </th>
        ))}
        {canEdit && <th className="action">Actions</th>}
      </tr>
    );
  }

  renderBody() {
    const rows = this.state.rows || [];
    const keys = this.state.keys || [];
    const canEdit = this.props.canEditBOQ;
    const selectedRowIds = this.props.selectedRowIds || [];
    // colSpan = keys + checkbox col + actions col (both only when canEdit)
    const colSpan = keys.length + (canEdit ? 2 : 0);

    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={colSpan + (canEdit ? 0 : 1)}>{messages.common.noRecords}</td>
        </tr>
      );
    }

    const result = [];
    rows.forEach((row, index) => {
      const cls = this.getStatusClass(row.status || 0);
      const isSelected = selectedRowIds.includes(row.id);
      const isExpanded = this.state.expandedRowId === row.id;

      result.push(
        <tr
          key={`row-${index}`}
          className={`${index === rows.length - 1 ? "row last" : "row"} boq-row-${cls}${isSelected ? ' boq-row-selected' : ''}`}
        >
          {canEdit && (
            <td style={{ width: 40 }}>
              <Checkbox
                size="small"
                checked={isSelected}
                onChange={() => this.props.onSelectRow(row.id)}
              />
            </td>
          )}
          {keys.map((key, idx) => this.renderCell(key, row, idx))}
          {canEdit && (
            <td data-label="Actions" style={{ whiteSpace: 'nowrap' }}>
              <IconButton
                size="small"
                title="Delete all work-area entries for this product"
                onClick={() => this.props.onDeleteBOQ(row)}
              >
                <DeleteIcon fontSize="small" style={{ color: '#c62828' }} />
              </IconButton>
            </td>
          )}
        </tr>
      );

      if (isExpanded) {
        result.push(this.renderExpansionRow(row, colSpan));
      }
    });

    return result;
  }
}

export default Table;
