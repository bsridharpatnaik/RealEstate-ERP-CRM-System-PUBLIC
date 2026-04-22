//react
import React from "react";
//third party
//style
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";
import Button from "@material-ui/core/Button";

class Table extends CommonTable {
  renderCell(key, row,index) {
    if (key === "buildingType") {
      const value = row["buildingType"] ? row["buildingType"]["typeName"] : "";
      return <td data-label={'Building Type'}>{value}</td>;
    } else {
      return super.renderCell(key, row,index);
    }
  }
}
export default Table;
