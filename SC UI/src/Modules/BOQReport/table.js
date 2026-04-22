//react
import React from "react";
//third party
//style
import "./style.scss";
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";
import Button from "@material-ui/core/Button";

class Table extends CommonTable {
  renderCell(key, row, headerName) {
    if (key === "productId") {
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
                  messages.common.totalStock,
                ],
                keys: ["productId", "productName", "totalQuantityInHand"],
              });
            }}
          >
            {`${row["productId"]}`}
          </Button>
        </td>
      );
    }
    else if (key === "status") {
      const value = row[key] != null ? row[key] + "%" : "";
      const className = value;
      return (
        <td data-label={"Status"}>
          <span className={className}>{`${value}`}</span>
        </td>
      );
    }
    else if (key === "totalQuantityInHand") {
      return (
        <td data-label={messages.common.totalStock}>
          {`${row["totalQuantityInHand"]} -
            ${row.detailedStock[0].measurementUnit}`}
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
