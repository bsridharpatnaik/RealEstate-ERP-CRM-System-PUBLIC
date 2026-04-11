import React from "react";
import CommonTable from "../../Shared/Table";
import { messages } from "../../messages";

class Table extends CommonTable {
  renderCell(key, row, index) {
    if (key === "quantity" || key === "currentStock") {
      const value = row[key] != null ? `${row[key]} - ${row.measurementUnit}` : "";
      const label =
        key === "quantity" ? messages.common.quantity : messages.common.currentStock;
      return <td data-label={label}>{value}</td>;
    }

    return super.renderCell(key, row, index);
  }
}

export default Table;
