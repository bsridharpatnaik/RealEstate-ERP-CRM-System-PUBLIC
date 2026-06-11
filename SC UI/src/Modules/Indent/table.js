//react
import React from "react";
//third party
import { withStyles } from "@material-ui/core/styles";
import Tooltip from "@material-ui/core/Tooltip";
import Typography from "@material-ui/core/Typography";
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
  checkDelete(row) {
    // Add delete constraint logic if needed
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
      return (
        <td data-label="Indent. No.">
          {row["indentId"]}
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
      const inventoryList = row.inventoryList || row.inventoryItems || [];
      const inventoryCount = row.inventoryCount || inventoryList.length || 0;
      
      // Check if there's expanded inventory details
      if (row.inventoryDetails && Array.isArray(row.inventoryDetails)) {
        return (
          <td data-label="Inventory Count">
            {row.inventoryDetails.map((item, idx) => (
              <div key={idx}>{item.productName} {item.quantity} {item.unit}</div>
            ))}
          </td>
        );
      }
      
      // Render with tooltip if inventory items exist
      if (inventoryList.length > 0) {
        const tooltipContent = (
          <div className="inventory-tooltip-content">
            <Typography className="inventory-tooltip-title">Inventory Details</Typography>
            <div className="inventory-tooltip-items">
              {inventoryList.map((item, idx) => {
                const productName = item.product?.productName || item.productName || "Unknown";
                const quantity = item.quantity || 0;
                const unit = item.measurementUnit || item.unit || "";
                return (
                  <div key={idx} className="inventory-tooltip-item">
                    <span className="inventory-tooltip-name">{productName}</span>
                    <span className="inventory-tooltip-quantity">{quantity} {unit}</span>
                  </div>
                );
              })}
            </div>
          </div>
        );
        
        return (
          <td data-label="Inventory Count">
            <HtmlTooltip title={tooltipContent} placement="top" arrow>
              <span className="inventory-count-hoverable">{inventoryCount}</span>
            </HtmlTooltip>
          </td>
        );
      }
      
      return <td data-label="Inventory Count">{inventoryCount}</td>;
    } else {
      return super.renderCell(key, row, index);
    }
  }

  renderBody() {
    const rows = this.state.rows || [];
    const keys = this.state.keys || [];
    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={keys.length + 1}>{messages.common.noRecords}</td>
        </tr>
      );
    }
    return rows.map((row, index) => (
      <tr
        key={index}
        className={index === rows.length - 1 ? "row last clickable-row" : "row clickable-row"}
        onClick={() => {
          this.props.showDetail(row);
        }}
      >
        {keys.map((key, index) => {
          return this.renderCell(key, row, index);
        })}
        {this.renderAction(row)}
      </tr>
    ));
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

