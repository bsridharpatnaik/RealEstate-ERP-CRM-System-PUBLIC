//react
import React from "react";
//third party
//style
import "./style.scss";
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";
import Button from "@material-ui/core/Button";
import { checkifDateLessThan, getRoleEditConstraintDays } from "./../../helper";

class Table extends CommonTable {
  checkDelete(row) {
    const days = getRoleEditConstraintDays();

    const isDisabled = checkifDateLessThan(row.date, days);
    return isDisabled;
  }
  renderCell(key, row,index) {
    if (key === "machinery") {
      return <td data-label={messages.fields.machinery}>{`${row.machinery.machineryName}`}</td>;
    } else if (key === "supplier") {
      const value = row.supplier ? row.supplier.name : "";
      return <td data-label={messages.fields.supplier}>{`${value}`}</td>;
    } else if (key === "usageLocation") {
      const value = row.usageLocation ? row.usageLocation.locationName : "";

      return <td data-label={messages.fields.location}>{`${value}`}</td>;
    } else if (key === "morid") {
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
                messages.fields.machinery,
                messages.fields.mode,
              ],
              keys: ["morid", "machinery", "mode"],
            });
          }}
        >
          {`${row["morid"]}`}
        </Button>
        </td>
      );
    } else {
      return super.renderCell(key, row,index);
    }
  }
}
export default Table;
