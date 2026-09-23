//react
import React from "react";
//third party
import Tooltip from "@material-ui/core/Tooltip";
import KeyboardArrowDownIcon from "@material-ui/icons/KeyboardArrowDown";
import KeyboardArrowUpIcon from "@material-ui/icons/KeyboardArrowUp";
import AttachFileIcon from "@material-ui/icons/AttachFile";
//style
import "./style.scss";
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";
import IconButton from "@material-ui/core/IconButton";
import trashOutlineIcon from "./../../Shared/Icons/trash-outline.png";
import eyeIcon from "./../../Shared/Icons/eye.png";
import pencilIcon from "./../../Shared/Icons/pencil.png";

class Table extends CommonTable {
  state = {
    ...this.state,
    expandedRowId: null,
  };

  toggleExpand = (id, e) => {
    e.stopPropagation();
    this.setState(prev => ({
      expandedRowId: prev.expandedRowId === id ? null : id,
    }));
  };

  isRowExpanded(id) {
    return this.props.allExpanded || this.state.expandedRowId === id;
  }

  checkDelete(row) {
    const statusValue = row.poStatus || row.status || "";
    const normalizedStatus = statusValue.toLowerCase().trim();
    return normalizedStatus === "cancelled" || normalizedStatus === "canceled";
  }

  renderHeader() {
    const headers = this.state.headers;
    return (
      <tr>
        {headers.map((header, index) => {
          const isInventoryCount = this.keys[index] === "inventoryCount";
          return (
            <th
              key={index}
              onClick={isInventoryCount ? undefined : () => this.sort(index)}
              className={isInventoryCount ? "" : "sortable"}
            >
              <span>
                {header} {this.renderSortIcon(index)}
              </span>
            </th>
          );
        })}
        <th colSpan="3" className="action">
          {messages.common.action}
        </th>
      </tr>
    );
  }

  renderAction(row) {
    const NON_EDITABLE_STATUSES = ["CANCELLED"];
    const isEditAvailable = !this.hideedit && !NON_EDITABLE_STATUSES.includes((row.poStatus || "").toUpperCase()) && this.props.edit;
    const isDeleteAvailable = !this.hidedelete && !(this.checkDelete && this.checkDelete(row));

    if (!isDeleteAvailable) {
      return (
        <td data-label="Action" className="action-single" colSpan="3">
          <Tooltip title="View">
            <IconButton
              aria-label="view"
              onClick={(e) => {
                e.stopPropagation();
                if (this.props.showDetail) this.props.showDetail(row);
              }}
              className="back-icon"
            >
              <img src={eyeIcon} alt="View" style={{ width: 15, height: 15 }} />
            </IconButton>
          </Tooltip>
        </td>
      );
    }

    return (
      <React.Fragment>
        <td data-label="View" className="action-view">
          <Tooltip title="View">
            <IconButton
              aria-label="view"
              onClick={(e) => {
                e.stopPropagation();
                if (this.props.showDetail) this.props.showDetail(row);
              }}
              className="back-icon"
            >
              <img src={eyeIcon} alt="View" style={{ width: 15, height: 15 }} />
            </IconButton>
          </Tooltip>
        </td>
        <td data-label="Edit" className="action-edit">
          {isEditAvailable && (
            <Tooltip title="Edit">
              <IconButton
                aria-label="edit"
                onClick={(e) => {
                  e.stopPropagation();
                  this.props.edit(row);
                }}
                className="back-icon"
              >
                <img src={pencilIcon} alt="Edit" style={{ width: 15, height: 15 }} />
              </IconButton>
            </Tooltip>
          )}
        </td>
        <td data-label="Delete" className="action-delete">
          <Tooltip title="Delete">
            <IconButton
              aria-label="delete"
              onClick={(e) => {
                e.stopPropagation();
                this.deleteRow = row;
                this.setState({ deleteConfirmOpen: true });
              }}
              className="back-icon"
            >
              <img src={trashOutlineIcon} alt="Delete" style={{ width: 15, height: 15 }} />
            </IconButton>
          </Tooltip>
        </td>
      </React.Fragment>
    );
  }

  renderCell(key, row, index) {
    if (key === "poNumber") {
      const expanded = this.isRowExpanded(row.poNumber);
      const hasLines = (row.lines || []).length > 0;
      return (
        <td data-label="PO Number" style={{ whiteSpace: 'nowrap' }}>
          {hasLines && (
            <IconButton
              size="small"
              onClick={(e) => this.toggleExpand(row.poNumber, e)}
              title={expanded ? 'Collapse items' : 'Expand items'}
              style={{ marginRight: 4 }}
            >
              {expanded
                ? <KeyboardArrowUpIcon fontSize="small" />
                : <KeyboardArrowDownIcon fontSize="small" />}
            </IconButton>
          )}
          <span style={{ verticalAlign: 'middle' }}>{row["poNumber"]}</span>
          {(row.fileInformations || []).length > 0 && (
            <AttachFileIcon
              fontSize="small"
              titleAccess="Has attachments"
              style={{ verticalAlign: 'middle', marginLeft: 4, color: '#888', fontSize: 16 }}
            />
          )}
        </td>
      );
    } else if (key === "poStatus") {
      const statusValue = row[key] || "";
      let statusClass = "";
      const normalizedStatus = statusValue.toLowerCase().trim();
      if (normalizedStatus === "new" || normalizedStatus === "created") {
        statusClass = "status-new";
      } else if (normalizedStatus === "cancelled" || normalizedStatus === "canceled") {
        statusClass = "status-cancelled";
      } else if (normalizedStatus === "complete inward" || normalizedStatus === "completeinward") {
        statusClass = "status-complete-inward";
      } else if (normalizedStatus === "short close" || normalizedStatus === "shortclose" || normalizedStatus === "short closed" || normalizedStatus === "shortclosed") {
        statusClass = "status-short-close";
      } else if (normalizedStatus === "partial" || normalizedStatus === "partial inward" || normalizedStatus === "partialinward") {
        statusClass = "status-partial";
      } else {
        statusClass = `status-${normalizedStatus.replace(/\s+/g, "-")}`;
      }
      return (
        <td data-label="PO Status" className="po-status-cell">
          <div className="po-status-cell-inner">
            <span className={`status-badge ${statusClass}`}>{statusValue}</span>
            {row.hasOverdueLines && (
              <span className="overdue-badge" title="One or more line items have exceeded lead time">⚠ Overdue</span>
            )}
          </div>
        </td>
      );
    } else if (key === "dateCreation") {
      return (
        <td data-label="Date Creation">{row["dateCreation"]}</td>
      );
    } else if (key === "inventoryCount") {
      const count = (row.lines || []).length || row.inventoryCount || 0;
      return (
        <td data-label="Items" style={{ textAlign: 'center' }}>
          <span style={{
            display: 'inline-block',
            minWidth: 24,
            padding: '1px 8px',
            borderRadius: 12,
            background: '#e3f2fd',
            color: '#1565c0',
            fontWeight: 600,
            fontSize: 12,
          }}>{count}</span>
        </td>
      );
    } else if (key === "supplierName") {
      return (
        <td data-label="Supplier Name" title={row["supplierName"]}>{row["supplierName"]}</td>
      );
    } else if (key === "createdBy") {
      return <td data-label="Created By">{row["createdBy"]}</td>;
    } else if (key === "projectName") {
      return <td data-label="Project">{row["projectName"] || "-"}</td>;
    } else if (key === "specialPo") {
      return (
        <td data-label="SPL" className="spl-po-cell">
          {row["specialPo"] ? <span className="spl-po-check">✓</span> : null}
        </td>
      );
    } else {
      return super.renderCell(key, row, index);
    }
  }

  renderExpansionRow(row, colSpan) {
    const lines = row.lines || [];
    return (
      <tr key={`expand-${row.poNumber}`} className="po-expansion-row">
        <td colSpan={colSpan} style={{ padding: 0, borderTop: '1px solid #e0e0e0' }}>
          <div style={{ background: '#f9fbff', padding: '10px 24px 14px' }}>
            {lines.length === 0 ? (
              <span style={{ color: '#888', fontSize: 13 }}>No line items.</span>
            ) : (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid #ddd' }}>
                    <th style={{ textAlign: 'left', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Product</th>
                    <th style={{ textAlign: 'right', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Qty</th>
                    <th style={{ textAlign: 'left', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Unit</th>
                    <th style={{ textAlign: 'right', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Rate</th>
                    <th style={{ textAlign: 'right', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Discount %</th>
                    <th style={{ textAlign: 'right', padding: '5px 10px', fontWeight: 600, color: '#555' }}>GST %</th>
                    <th style={{ textAlign: 'right', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Net Rate</th>
                    <th style={{ textAlign: 'right', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Amount</th>
                    <th style={{ textAlign: 'center', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Line Status</th>
                  </tr>
                </thead>
                <tbody>
                  {lines.filter(l => !l.deleted).map((line, i) => {
                    const product = line.product || {};
                    const productName = product.productName || line.productName || '—';
                    const unit = product.measurementUnit || line.measurementUnit || '';
                    const qty = line.quantity != null ? line.quantity : '—';
                    const rate = line.rate != null ? `₹${Number(line.rate).toFixed(2)}` : '—';
                    const discountPercent = line.discountPercent != null ? `${Number(line.discountPercent).toFixed(2)}%` : '—';
                    const gstPercent = line.gstPercent != null ? `${Number(line.gstPercent).toFixed(2)}%` : '—';
                    const netRate = line.netRate != null ? `₹${Number(line.netRate).toFixed(2)}` : '—';
                    const total = line.totalAmount != null ? `₹${Number(line.totalAmount).toFixed(2)}` : '—';
                    const lineStatus = line.lineItemStatus || '';
                    const leadTime = line.leadTimeDays;
                    const daysLeft = line.daysLeft;
                    const isOverdue = line.isOverdue;
                    return (
                      <tr key={i} style={{ borderBottom: '1px solid #eee' }}>
                        <td style={{ padding: '5px 10px' }}>
                          <span>{productName}</span>
                          {leadTime != null && (
                            <span style={{ marginLeft: 8, fontSize: 11, color: '#888' }}>
                              ⏱ {leadTime}d
                            </span>
                          )}
                          {daysLeft != null && (
                            <span style={{
                              marginLeft: 6,
                              fontSize: 11,
                              fontWeight: 600,
                              padding: '1px 6px',
                              borderRadius: 10,
                              background: isOverdue ? '#fdecea' : '#eafaf1',
                              color: isOverdue ? '#c0392b' : '#1e8449',
                            }}>
                              {isOverdue ? `${Math.abs(daysLeft)}d overdue` : `${daysLeft}d left`}
                            </span>
                          )}
                        </td>
                        <td style={{ padding: '5px 10px', textAlign: 'right' }}>{qty}</td>
                        <td style={{ padding: '5px 10px' }}>{unit}</td>
                        <td style={{ padding: '5px 10px', textAlign: 'right' }}>{rate}</td>
                        <td style={{ padding: '5px 10px', textAlign: 'right' }}>{discountPercent}</td>
                        <td style={{ padding: '5px 10px', textAlign: 'right' }}>{gstPercent}</td>
                        <td style={{ padding: '5px 10px', textAlign: 'right' }}>{netRate}</td>
                        <td style={{ padding: '5px 10px', textAlign: 'right' }}>{total}</td>
                        <td style={{ padding: '5px 10px', textAlign: 'center' }}>
                          {lineStatus
                            ? <span className={`status-badge status-${lineStatus.toLowerCase().replace(/\s+/g, '-')}`}>{lineStatus}</span>
                            : '—'}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </td>
      </tr>
    );
  }

  renderBody() {
    const rows = this.state.rows || [];
    const keys = this.state.keys;
    const colSpan = keys.length + 3;
    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={colSpan}>{messages.common.noRecords}</td>
        </tr>
      );
    }
    return rows.map((row, index) => {
      const isExpanded = this.isRowExpanded(row.poNumber);
      return [
        <tr
          key={index}
          className={index === rows.length - 1 ? "row last" : "row"}
        >
          {keys.map((key, i) => this.renderCell(key, row, i))}
          {this.renderAction(row)}
        </tr>,
        isExpanded && this.renderExpansionRow(row, colSpan),
      ];
    });
  }
}

export default Table;
