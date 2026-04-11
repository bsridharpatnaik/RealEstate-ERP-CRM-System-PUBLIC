import React from "react";
import "./style.scss";
import ExpandTable from "./../../Shared/Table";

class Table extends ExpandTable {
  renderCell(key, row, index) {
    if (key === "quantity" || key === "closingStock") {
      const value = row[key] + " - " + row["measurementUnit"];
      return <td data-label={key}>{`${value}`}</td>;
    } else {
      return super.renderCell(key, row, index);
    }
  }
}
export default Table;
