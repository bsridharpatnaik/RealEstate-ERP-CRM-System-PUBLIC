//react
import React from "react";
//style
import "./style.scss";
import Button from "@material-ui/core/Button";
import { withRouter } from "react-router-dom";
//misc
import CommonTable from "./../../../Shared/Table";
import { setSession } from '../../../helper';
import moment from "moment";
import { constants, messages } from "../../../messages";
class Table extends CommonTable {
  renderCell(key, row,index) {
    const value = row[key] !== null ? row[key] : "";
    if (key === 'leadId') {
      return (
        <td data-label={messages.fields.payments.customerId}>
          <Button
            color="primary"
            onClick={() => {
              setSession('isDealStructure', 1);
              setSession('currentPropertyId', row['dealStructureId']);
              this.props.history.push("/crm/customerinfo/" + row['leadId']);
            }}
          >
            <span>{`${value}`}</span>
          </Button>
        </td>
      );
    } else if (key === "isReceived") {
      const className = value ? "Received" : "Pending";
      return (
        <td data-label={messages.fields.payments.customerName}>
          <span className={className}>{`${value ? 'Yes' : 'No'}`}</span>
        </td>
      );
    } else if (key === "isCustomerPayment") {
      return (
        <td >
          <span className="">{`${value ? 'Customer' : 'Bank'}`}</span>
        </td>
      )
    } else if (key === "paymentDate") {
      let status;
      let date = moment(row.paymentDate, constants.dateFormat);
      const todaysDate = moment().format(constants.dateFormat);
      const todayDate = moment(moment(todaysDate, constants.dateFormat).add(1, 'days').add(-1, 'minutes').format(constants.dateTimeFormat), constants.dateTimeFormat);
      if (date < todayDate) {
        status = "pending";
      } else if (date > todayDate) {
        status = "upcoming";
      } else {
        status = "today";
      }
      return (
        <td data-label={messages.fields.payments.paymentDate}>
          {row?.paymentDate ? <span className={"status " + status}>{row?.paymentDate ?? '-'}</span> : ''}
        </td>
      );
    } else {
      return super.renderCell(key, row,index);
    }
  }
}
export default withRouter(Table);
