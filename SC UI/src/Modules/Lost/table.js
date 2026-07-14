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
      const isExcess = row.entryType === "EXCESS_FOUND";
      return (
        <td data-label="Type">
          <span style={{
            display: 'inline-block',
            padding: '2px 10px',
            borderRadius: 12,
            fontSize: 12,
            fontWeight: 600,
            background: isExcess ? '#e8f5e9' : '#ffebee',
            color: isExcess ? '#2e7d32' : '#c62828',
            border: `1px solid ${isExcess ? '#a5d6a7' : '#ef9a9a'}`,
            whiteSpace: 'nowrap',
          }}>
            {isExcess ? '↑ Excess Found' : '↓ Lost / Damaged'}
          </span>
        </td>
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
