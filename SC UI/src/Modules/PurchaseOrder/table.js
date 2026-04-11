//react
import React from "react";
//third party
import Tooltip from "@material-ui/core/Tooltip";
//style
import "./style.scss";
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";
import IconButton from "@material-ui/core/IconButton";
import trashOutlineIcon from "./../../Shared/Icons/trash-outline.png";
import eyeIcon from "./../../Shared/Icons/eye.png";

class Table extends CommonTable {
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
        <th colSpan="2" className="action">
          {messages.common.action}
        </th>
      </tr>
    );
  }

  renderAction(row) {
    const isDeleteAvailable = !this.hidedelete && !(this.checkDelete && this.checkDelete(row));

    // When delete is NOT available, center the single action under the full Action column
    if (!isDeleteAvailable) {
      return (
        <td data-label="Action" className="action-single" colSpan="2">
          <Tooltip title="View">
            <IconButton
              aria-label="view"
              onClick={(e) => {
                e.stopPropagation();
                if (this.props.showDetail) {
                  this.props.showDetail(row);
                }
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
                if (this.props.showDetail) {
                  this.props.showDetail(row);
                }
              }}
              className="back-icon"
            >
              <img src={eyeIcon} alt="View" style={{ width: 15, height: 15 }} />
            </IconButton>
          </Tooltip>
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
      return (
        <td data-label="PO Number">
          {row["poNumber"]}
        </td>
      );
    } else if (key === "poStatus") {
      const statusValue = row[key] || "";
      let statusClass = "";

      const normalizedStatus = statusValue.toLowerCase().trim();

      // Map PO status values to CSS classes
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
          </div>
        </td>
      );
    } else if (key === "dateCreation") {
      return (
        <td data-label="Date Creation">
          {row["dateCreation"]}
        </td>
      );
    } else if (key === "inventoryCount") {
      return (
        <td data-label="Inventory Count">
          {row["inventoryCount"] || 0}
        </td>
      );
    } else if (key === "supplierName") {
      return (
        <td data-label="Supplier Name">
          {row["supplierName"]}
        </td>
      );
    } else if (key === "createdBy") {
      return (
        <td data-label="Created By">
          {row["createdBy"]}
        </td>
      );
    } else if (key === "priority") {
      const priority = row["priority"];
      const days = row["daysToDeadline"];
      if (!priority) return <td data-label="Priority">-</td>;
      let badgeClass = "priority-badge-normal";
      if (priority === "CRITICAL") badgeClass = "priority-badge-critical";
      else if (priority === "HIGH") badgeClass = "priority-badge-high";
      else if (priority === "MEDIUM") badgeClass = "priority-badge-medium";
      const daysLabel = days !== null && days !== undefined
        ? (days < 0 ? ` (${Math.abs(days)}d overdue)` : ` (${days}d)`)
        : "";
      return (
        <td data-label="Priority">
          <span className={`priority-badge ${badgeClass}`}>
            {priority}{daysLabel}
          </span>
        </td>
      );
    } else if (key === "specialPo") {
      return (
        <td data-label="SPL" className="spl-po-cell">
          {row["specialPo"] ? (
            <span className="spl-po-check">✓</span>
          ) : null}
        </td>
      );
    } else {
      return super.renderCell(key, row, index);
    }
  }

  renderBody() {
    const rows = this.state.rows || [];
    const keys = this.state.keys;
    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={keys.length + 2}>{messages.common.noRecords}</td>
        </tr>
      );
    }
    return rows.map((row, index) => (
      <tr
        key={index}
        className={index === rows.length - 1 ? "row last clickable-row" : "row clickable-row"}
        onClick={() => {
          if (this.props.showDetail) {
            this.props.showDetail(row);
          }
        }}
      >
        {keys.map((key, index) => {
          return this.renderCell(key, row, index);
        })}
        {this.renderAction(row)}
      </tr>
    ));
  }
}

export default Table;
