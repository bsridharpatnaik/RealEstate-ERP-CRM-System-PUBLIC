//react
import React from "react";
//third party
import { withStyles } from "@material-ui/core/styles";
import Tooltip from "@material-ui/core/Tooltip";
import Typography from "@material-ui/core/Typography";
import KeyboardArrowDownIcon from "@material-ui/icons/KeyboardArrowDown";
import KeyboardArrowUpIcon from "@material-ui/icons/KeyboardArrowUp";
//style
import "./style.scss";
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";
import IconButton from "@material-ui/core/IconButton";
import trashOutlineIcon from "./../../Shared/Icons/trash-outline.png";
import eyeIcon from "./../../Shared/Icons/eye.png";
import pencilIcon from "./../../Shared/Icons/pencil.png";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import Dialog from "@material-ui/core/Dialog";
import Button from "./../../Shared/Button";

const HtmlTooltip = withStyles((theme) => ({
  tooltip: {
    backgroundColor: "#ffffff",
    color: "rgba(0, 0, 0, 0.87)",
    maxWidth: 300,
    fontSize: theme.typography.pxToRem(14),
    border: "none",
    borderRadius: "8px",
    boxShadow: "0 2px 8px rgba(0, 0, 0, 0.15)",
    padding: 0,
  },
}))(Tooltip);

class Table extends CommonTable {
  state = {
    ...this.state,
    expandedRowId: null,
    deleteConfirmOpen: false,
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
    return false;
  }

  renderHeader() {
    const headers = this.state.headers || [];
    if (!headers.length) {
      return (
        <tr>
          <th colSpan="6" className="action">
            {messages.common.action}
          </th>
        </tr>
      );
    }
    return (
      <tr>
        {headers.map((header, index) => {
          const isInventoryCount = this.keys && (this.keys[index] === "inventoryCount" || this.keys[index] === "poNumbers");
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
        <th className="action">
          {messages.common.action}
        </th>
      </tr>
    );
  }
  renderAction(row) {
    return (
      <td className="action-cell" data-label={messages.common.action}>
        <div className="action-buttons-wrapper">
          <IconButton
            aria-label="view"
            onClick={() => {
              this.props.showDetail(row);
            }}
            className="back-icon action-btn"
          >
            <img src={eyeIcon} alt="View" style={{ width: 15, height: 15 }} />
          </IconButton>
          {(this.props.canEditIndentRow ? this.props.canEditIndentRow(row) : !this.hideedit) && (
            <IconButton
              aria-label="edit"
              onClick={(event) => {
                event.stopPropagation();
                this.props.edit(row);
              }}
              className="back-icon action-btn"
              disabled={this.checkDelete ? this.checkDelete(row) : undefined}
            >
              <img src={pencilIcon} alt="Edit" style={{ width: 15, height: 15 }} />
            </IconButton>
          )}
          {(this.props.canDeleteIndentRow ? this.props.canDeleteIndentRow(row) : !this.hidedelete) && (
            <IconButton
              aria-label="delete"
              onClick={(event) => {
                event.stopPropagation();
                this.deleteRow = row;
                this.setState({ deleteConfirmOpen: true });
              }}
              disabled={this.checkDelete ? this.checkDelete(row) : undefined}
              className="back-icon action-btn"
            >
              <img src={trashOutlineIcon} alt="Delete" style={{ width: 15, height: 15 }} />
            </IconButton>
          )}
        </div>
      </td>
    );
  }
  renderCell(key, row, index) {
    if (key === "daysRemaining") {
      const days = row["daysRemaining"];
      const normalizedStatus = (row["status"] || "").toLowerCase().trim();
      const isTerminal = normalizedStatus === "cancelled" || normalizedStatus === "rejected" || normalizedStatus === "po completed" || normalizedStatus === "closed" || normalizedStatus === "short closed";
      if (days === null || days === undefined || isTerminal) {
        return <td data-label="Days Left">-</td>;
      }
      let badgeClass = "days-badge-normal";
      if (days <= 3) badgeClass = "days-badge-critical";
      else if (days <= 7) badgeClass = "days-badge-high";
      else if (days <= 14) badgeClass = "days-badge-medium";
      const label = days < 0 ? `${Math.abs(days)}d overdue` : `${days}d`;
      return (
        <td data-label="Days Left">
          <span className={`days-badge ${badgeClass}`}>{label}</span>
        </td>
      );
    } else if (key === "indentId") {
      const expanded = this.isRowExpanded(row.indentId);
      const hasItems = (row.inventoryList || row.inventoryItems || []).length > 0;
      return (
        <td data-label="Indent. No." style={{ whiteSpace: 'nowrap' }}>
          {hasItems && (
            <IconButton
              size="small"
              onClick={(e) => this.toggleExpand(row.indentId, e)}
              title={expanded ? 'Collapse items' : 'Expand items'}
              style={{ marginRight: 4 }}
            >
              {expanded
                ? <KeyboardArrowUpIcon fontSize="small" />
                : <KeyboardArrowDownIcon fontSize="small" />}
            </IconButton>
          )}
          <span style={{ verticalAlign: 'middle' }}>{row["indentId"]}</span>
        </td>
      );
    } else if (key === "projectName") {
      return (
        <td data-label="Project">
          {row["projectName"] || "-"}
        </td>
      );
    } else if (key === "status") {
      const statusValue = row[key] || "";
      let statusClass = "";
      
      // Normalize status value for comparison
      const normalizedStatus = statusValue.toLowerCase().trim();
      
      // Map indentStatus values to CSS classes
      if (normalizedStatus === "new") {
        statusClass = "status-new";
      } else if (normalizedStatus === "approved") {
        statusClass = "status-approved";
      } else if (normalizedStatus === "po partial") {
        statusClass = "status-po-partial";
      } else if (normalizedStatus === "po completed") {
        statusClass = "status-po-completed";
      } else if (normalizedStatus === "cancelled") {
        statusClass = "status-cancelled";
      } else if (normalizedStatus.includes("created")) {
        // Legacy support
        statusClass = "status-indent-created";
      } else if (normalizedStatus.includes("raised")) {
        // Legacy support
        statusClass = "status-po-raised";
      } else {
        // Fallback: create class from status value
        statusClass = `status-${normalizedStatus.replace(/\s+/g, '-')}`;
      }
      
      return (
        <td data-label="Status">
          <span className={`status-badge ${statusClass}`}>{statusValue}</span>
        </td>
      );
    } else if (key === "poNumbers") {
      const poNums = row.poNumbers || [];
      return (
        <td data-label="PO Numbers">
          {poNums.length > 0
            ? <div className="po-numbers-cell">
                {poNums.map((po) => (
                  <span key={po} className="po-number-badge">{po}</span>
                ))}
              </div>
            : "-"}
        </td>
      );
    } else if (key === "inventoryCount") {
      const count = row.inventoryCount || (row.inventoryList || row.inventoryItems || []).length || 0;
      return (
        <td data-label="Inventory Count" style={{ textAlign: 'center' }}>
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
    } else {
      return super.renderCell(key, row, index);
    }
  }

  renderExpansionRow(row, colSpan) {
    const items = row.inventoryList || row.inventoryItems || [];
    return (
      <tr key={`expand-${row.indentId}`} className="indent-expansion-row">
        <td colSpan={colSpan} style={{ padding: 0, borderTop: '1px solid #e0e0e0' }}>
          <div style={{ background: '#f9fbff', padding: '10px 24px 14px' }}>
            {items.length === 0 ? (
              <span style={{ color: '#888', fontSize: 13 }}>No line items.</span>
            ) : (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid #ddd' }}>
                    <th style={{ textAlign: 'left', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Product</th>
                    <th style={{ textAlign: 'right', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Qty</th>
                    <th style={{ textAlign: 'left', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Unit</th>
                    <th style={{ textAlign: 'left', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Specification</th>
                    <th style={{ textAlign: 'center', padding: '5px 10px', fontWeight: 600, color: '#555' }}>Line Status</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item, i) => {
                    const productName = item.product?.productName || item.productName || '—';
                    const qty = item.quantity != null ? item.quantity : '—';
                    const unit = item.measurementUnit || item.unit || item.product?.measurementUnit || '';
                    const spec = item.specification || '';
                    const lineStatus = item.lineItemStatus || '';
                    const leadTime = item.leadTimeDays;
                    return (
                      <tr key={i} style={{ borderBottom: '1px solid #eee' }}>
                        <td style={{ padding: '5px 10px' }}>
                          <span>{productName}</span>
                          {leadTime != null && (
                            <span style={{ marginLeft: 8, fontSize: 11, color: '#888' }}>
                              ⏱ {leadTime}d
                            </span>
                          )}
                        </td>
                        <td style={{ padding: '5px 10px', textAlign: 'right' }}>{qty}</td>
                        <td style={{ padding: '5px 10px' }}>{unit}</td>
                        <td style={{ padding: '5px 10px', color: '#666' }}>{spec || '—'}</td>
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
    const keys = this.state.keys || [];
    const colSpan = keys.length + 1;
    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={colSpan}>{messages.common.noRecords}</td>
        </tr>
      );
    }
    return rows.map((row, index) => {
      const isExpanded = this.isRowExpanded(row.indentId);
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

  render() {
    return (
      <div className="table-wrapper">
        <table className="update-table">
          <thead>{this.renderHeader()}</thead>
          <tbody>{this.renderBody()}</tbody>
        </table>
        {/* Delete Confirmation Dialog */}
        <Dialog
          disableBackdropClick
          disableEscapeKeyDown
          maxWidth="xs"
          aria-labelledby="cancel-confirmation-dialog-title"
          open={this.state.deleteConfirmOpen}
        >
          <DialogTitle id="cancel-confirmation-dialog-title">Cancel Indent</DialogTitle>
          <DialogContent dividers>
            Are you sure you want to cancel this indent? This action cannot be undone.
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => {
                this.deleteRow = null;
                this.setState({ deleteConfirmOpen: false });
              }}
              buttonClass="grey"
              label="No"
            ></Button>
            <Button
              onClick={() => {
                this.props.delete(this.deleteRow);
                this.setState({ deleteConfirmOpen: false });
              }}
              buttonClass="red"
              label="Yes, Cancel"
            ></Button>
          </DialogActions>
        </Dialog>
      </div>
    );
  }
}
export default Table;

