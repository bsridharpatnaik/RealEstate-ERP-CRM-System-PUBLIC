//react
import React from "react";
//third party
//style
import "./style.scss";
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";
import Tooltip from "@material-ui/core/Tooltip";

class Table extends CommonTable {
  renderBody() {
    const rows = this.state.rows || [];
    const keys = this.state.keys;
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
        className={index === rows.length - 1 ? "row last" : "row"}
        style={{ cursor: "pointer" }}
        onClick={() => this.props.showDetail(row)}
      >
        {keys.map((key, i) => this.renderCell(key, row, i))}
        {this.renderAction(row)}
      </tr>
    ));
  }

  renderCell(key, row, headerName) {
    if (key === "productId") {
      return (
        <td data-label={messages.common.id}>
          {`${row["productId"]}`}
        </td>
      );
    } else if (key === "totalQuantityInHand") {
      return (
        <td data-label={messages.common.totalStock}>
          {`${row["totalQuantityInHand"]} -
            ${row.detailedStock[0].measurementUnit}`}
        </td>
      );
    } else if (key === "warehouseCount") {
      const active = row.detailedStock.filter((w) => w.quantityInHand > 0);
      const tip = (
        <div style={{ fontSize: "13px", lineHeight: "1.8" }}>
          {active.map((w) => (
            <div key={w.warehouseName}>
              {w.warehouseName}: {w.quantityInHand} {w.measurementUnit}
            </div>
          ))}
        </div>
      );
      return (
        <td data-label="Warehouses">
          <Tooltip title={tip} arrow interactive>
            <span style={{ cursor: "default", color: "#007bff", fontWeight: "bold" }}>
              {active.length}
            </span>
          </Tooltip>
        </td>
      );
    } else if (key === "stockStatus") {
      const value = row[key] !== null ? row[key] : "";

      const className = value.toLowerCase();
      return (
        <td data-label={messages.common.stockStatus}>
          <span className={className}>{`${value}`}</span>
        </td>
      );
    } else {
      return super.renderCell(key, row, headerName);
    }
  }
}
export default Table;
