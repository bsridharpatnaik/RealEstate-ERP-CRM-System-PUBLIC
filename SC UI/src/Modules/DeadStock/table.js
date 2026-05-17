//react
import React from "react";
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";

class Table extends CommonTable {
  renderAction() {
    return null;
  }

  renderCell(key, row, name) {
    if (key === "quantityInHand") {
      const qty = row._quantityInHand;
      const reorder = row._reorderLevel;

      let badge = null;
      if (qty != null && reorder != null) {
        if (qty <= reorder) {
          badge = <span className="stock-badge stock-badge--low">Low Stock</span>;
        } else {
          badge = <span className="stock-badge stock-badge--ok">In Stock</span>;
        }
      }

      return (
        <td data-label={this.state.headers[name] || key}>
          {`${row[key]}`}
          {badge}
        </td>
      );
    }
    return super.renderCell(key, row, name);
  }
}

export default Table;
