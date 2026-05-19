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
  renderCell(key, row, index) {
    if (key === "supplier") {
      return <td data-label={key}>{`${row["supplier"]?.name || ""}`}</td>;
    } else if (key === "warehouse") {
      return <td data-label={key}>{`${row["warehouse"]?.warehouseName || ""}`}</td>;
    } else if (key === "inventoryCount") {
      return <td data-label={key}>{`${row["inwardOutwardList"].length}`}</td>;

    } else if (key === "inwardType") {
      // Derive type from the two boolean flags on the row
      let label, color, background;
      if (row.isSampleInward) {
        label = "Sample Inward";
        color = "#6a1b9a";
        background = "#f3e5f5";
      } else if (row.createdFromPO) {
        label = "From PO";
        color = "#1565c0";
        background = "#e3f2fd";
      } else {
        label = "Direct Inward";
        color = "#2e7d32";
        background = "#e8f5e9";
      }
      return (
        <td data-label="Inward Type">
          <span style={{
            display: 'inline-block',
            padding: '3px 10px',
            borderRadius: '12px',
            fontSize: '11px',
            fontWeight: 600,
            color: color,
            backgroundColor: background,
            border: `1px solid ${color}`,
            letterSpacing: '0.3px',
            whiteSpace: 'nowrap',
          }}>
            {label}
          </span>
        </td>
      );

    } else if (key === "missingChallanBillFlag") {
      const missing = (!row.challanNo || !row.challanNo.trim()) && (!row.billNo || !row.billNo.trim());
      if (!missing) return <td data-label="Doc Status"></td>;
      return (
        <td data-label="Doc Status">
          <span
            title={row.noChallanBillReason || ''}
            style={{
              display: 'inline-block',
              padding: '3px 10px',
              borderRadius: '12px',
              fontSize: '11px',
              fontWeight: 600,
              color: '#b71c1c',
              backgroundColor: '#ffebee',
              border: '1px solid #b71c1c',
              whiteSpace: 'nowrap',
              cursor: row.noChallanBillReason ? 'help' : 'default',
            }}>
            No Challan/Bill
          </span>
        </td>
      );
    } else if (key === "inwardId") {
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
                  messages.fields.supplier,
                ],
                keys: ["inwardId", "date", "supplier"],
              });
            }}
          >
            {`${row["inwardId"]}`}
          </Button>
        </td>
      );
    } else {
      return super.renderCell(key, row, index);
    }
  }
}
export default Table;
