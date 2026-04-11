//react
import React from "react";
//third party
import Button from "@material-ui/core/Button";
//style
import "./style.scss";
//misc
import { messages } from "./../../messages";
import CommonTable from "./../../Shared/Table";
class Table extends CommonTable {
  renderCell(key, row, index) {
    const value = row[key] !== null ? row[key] : "";
    if (key === "contactType") {
      const className = value.toLowerCase();
      return (
        <td data-label={key}>
          <span className={className}>{`${value}`}</span>
        </td>
      );
    } else if (key === "name") {
      return (
        <td data-label={messages.fields.contactName}>
          <Button
            color="primary"
            onClick={() => {
              this.hideedit = true;
              this.hidedelete = true;
              this.props.showDetail(row);
              this.setState({
                headers: [
                  messages.fields.contactName,
                  messages.fields.mobileNo,
                  messages.fields.contactType,
                ],
                keys: ["name", "mobileNo", "contactType"],
              });
            }}
          >
            {value}
          </Button>
        </td>
      );
    } else {
      return super.renderCell(key, row, index);
    }
  }
}
export default Table;
