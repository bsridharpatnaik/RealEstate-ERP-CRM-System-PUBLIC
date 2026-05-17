//react
import React from "react";
//third party
//style
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";
import Button from "@material-ui/core/Button";
class Table extends CommonTable {
  renderCell(key, row, index) {
    if (key === "warehouse") {
      return (
        <td
          data-label={messages.common.id}
        >{`${row.warehouse.warehouseName}`}</td>
      );
    } else if (key === "product") {
      return (
        <td
          data-label={messages.common.warehouse}
        >{`${row.product.productName}`}</td>
      );
    } else if (key === "category") {
      return (
        <td
          data-label={messages.common.inventory}
        >{`${row.product.category.categoryName}`}</td>
      );
    } else if (key === "unit") {
      return (
        <td data-label={"Quantity"}>{`${row.product.measurementUnit}`}</td>
      );
    } else if (key === "entryType") {
      const label = row.entryType === "EXCESS_FOUND" ? "Excess Found" : "Lost / Damaged";
      return (
        <td data-label="Type">{label}</td>
      );
    } else if (key === "id") {
      return (
        <td data-label={messages.common.id}>
          <Button
            color="primary"
            onClick={() => {
              this.hideedit = true;
              this.hidedelete = true;
              this.props.showDetail(row);
              this.setState({
                headers: [
                  messages.common.id,
                  messages.fields.date,
                  messages.common.warehouse,
                  messages.common.inventory,
                ],
                keys: ["id", "date", "warehouse", "product"],
              });
            }}
          >
            {`${row["id"]}`}
          </Button>
        </td>
      );
    } else {
      return super.renderCell(key, row, index);
    }
  }
}
export default Table;
