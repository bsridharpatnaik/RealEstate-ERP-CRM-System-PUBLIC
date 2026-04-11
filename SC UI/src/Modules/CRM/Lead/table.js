//react
import React from "react";
//third party
import Button from "@material-ui/core/Button";
//style
import moment from "moment";
//misc
import { messages, constants } from "./../../../messages";
import CommonTable from "./../../../Shared/Table";
import "./style.scss";
import { withRouter } from "react-router-dom";
import { setSession } from "../../../helper";

class Table extends CommonTable {
  renderCell(key, row, index) {
    const value = row[key] !== null ? row[key] : "";
    if (key === messages.formParams.lead.leadId) {
      return (
        <td data-label={messages.common.id}>
          <Button
            color="primary"
            onClick={() => {
              setSession("leadfilterdata", this.props.filterData);
              setSession("leadfilterPage", this.props.pageNumber);
              if (row.leadStatus === "Deal_Closed") {
                this.props.history.push("/crm/customerinfo/" + value);
              } else {
                this.props.history.push("/crm/leadinfo/" + value);
              }
            }}
          >
            <span>{`${value}`}</span>
          </Button>
        </td>
      );
    } else if (key === "nextPaymentDate") {
      let status;
      if (row["isOpen"]) {
        let date = moment(row.nextPaymentDate, constants.dateFormat);
        const todaysDate = moment().format(constants.dateFormat);
        const todayDate = moment(
          moment(todaysDate, constants.dateFormat)
            .add(1, "days")
            .add(-1, "minutes")
            .format(constants.dateTimeFormat),
          constants.dateTimeFormat
        );
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
        <td data-label={key}>
          {row?.nextPaymentDate ? (
            <span className={"status " + status}>{row?.nextPaymentDate}</span>
          ) : (
            ""
          )}
        </td>
      );
    } else if (key === "activityStatus") {
      let status;
      if (row["isOpen"]) {
        let date = moment(row.activityDateTime, constants.dateTimeFormat);
        const todaysDate = moment().format(constants.dateFormat);
        const todayDate = moment(
          moment(todaysDate, constants.dateFormat)
            .add(1, "days")
            .add(-1, "minutes")
            .format(constants.dateTimeFormat),
          constants.dateTimeFormat
        );
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
        <td data-label={key}>
          <span className={"status " + status}>{status}</span>
        </td>
      );
    } else {
      return super.renderCell(key, row, index);
    }
  }
}
export default withRouter(Table);
