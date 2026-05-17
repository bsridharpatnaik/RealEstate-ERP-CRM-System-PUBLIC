//react
import React from "react";
//third party
import Button from "@material-ui/core/Button";
//style
//misc
import { messages } from "./../../../messages";
import CommonTable from "./../../../Shared/Table";

import { withRouter } from "react-router-dom";
import { setSession } from '../../../helper';

class Table extends CommonTable {
  renderCell(key, row) {
    const value = row[key] !== null ? row[key] : "";
    if (key === messages.formParams.lead.leadId) {
      return (
        <td>
          <Button
            color="primary"
            onClick={() => {
              setSession('postSalesfilterdata', this.props.filterData);
              setSession('postSalesfilterPage', this.props.pageNumber);
              this.props.history.push("/crm/customerinfo/" + value);
            }}
          >
            <span>{`${value}`}</span>
          </Button>
        </td>
      );
    } else if (key === "activityStatus") {
      let status;
      if (row["isOpen"]) {
        let date = row.activityDateTime.split(" ");
        date = date[0] ? new Date(date[0]) : null;
        const todayDate = new Date();

        if (date < todayDate) {
          status = "pending";
        } else if (date > todayDate) {
          status = "upcoming";
        } else {
          status = "today";
        }
      } else {
        status = "completed";
      }
      return (
        <td>
          <span className={"status " + status}>{status}</span>
        </td>
      );
    } else {
      return super.renderCell(key, row);
    }
  }
}
export default withRouter(Table);
