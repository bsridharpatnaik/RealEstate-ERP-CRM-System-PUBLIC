//react
import React from "react";
//third party
//style
import "./style.scss";
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";
import Button from "@material-ui/core/Button";
import IconButton from "@material-ui/core/IconButton";
import EditIcon from "@material-ui/icons/Edit";

class Table extends CommonTable {

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

  renderCell(key, row, headerIndex) {
    if (key === "id") {
      return (
        <td data-label={messages.common.id}>
          <Button
            color="primary"
            onClick={() => {
              this.props.showDetail(row);
              this.setState({
                headers: [
                  messages.common.id,
                  messages.common.inventory,
                  messages.common.boqQuantity,
                ],
                keys: ["id", "product", "boqQuantity"],
              });
            }}
          >
            {`${row["id"]}`}
          </Button>
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
    } else {
      return super.renderCell(key, row, headerIndex);
    }
  }

  renderBody() {
    const rows = this.state.rows || [];
    const keys = this.state.keys;
    const canEdit = this.props.canEditBOQ;
    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={keys.length + (canEdit ? 2 : 1)}>{messages.common.noRecords}</td>
        </tr>
      );
    }
    return rows.map((row, index) => {
      const cls = this.getStatusClass(row.status || 0);
      return (
        <tr
          key={index}
          className={`${index === rows.length - 1 ? "row last" : "row"} boq-row-${cls}`}
        >
          {keys.map((key, idx) => this.renderCell(key, row, idx))}
          {canEdit && (
            <td data-label="Actions" style={{ whiteSpace: 'nowrap' }}>
              <IconButton
                size="small"
                title="Edit BOQ"
                onClick={() => this.props.onEditBOQ(row)}
              >
                <EditIcon fontSize="small" />
              </IconButton>
            </td>
          )}
          {this.renderAction(row)}
        </tr>
      );
    });
  }
}

export default Table;
