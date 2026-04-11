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
    if (key === "contractor") {
      return <td data-label={key}>{`${row["contractor"]["name"]}`}</td>;
    } else if (key === "warehouse") {
      return <td data-label={key}>{`${row["warehouse"]["warehouseName"]}`}</td>;
    } else if (key === "inventoryCount") {
      return <td data-label={key}>{`${row["inwardOutwardList"].length}`}</td>;
    } else if (key === "usageLocation") {
      return <td data-label={messages.common.location}>{`${row["usageLocation"]["locationName"]}`}</td>;
    } else if (key === "usageArea") {
      return <td data-label={messages.common.finalLocation}>{`${row["usageArea"]["usageAreaName"]}`}</td>;
    } else if (key === "outwardid") {
      return (
        <td data-label='ID'>
        <Button 
        
          color="primary"
          onClick={() => {
            this.hideedit = true;
            this.hidedelete = true;
            this.props.showDetail(row);
            this.setState({
              headers: [messages.common.id, messages.fields.date, "Contractor"],
              keys: ["outwardid", "date", "contractor"],
            });
          }}
        >
          {`${row["outwardid"]}`}
        </Button>
        </td>
      );
    } else {
      return super.renderCell(key, row,index);
    }
  }
}
export default Table;
