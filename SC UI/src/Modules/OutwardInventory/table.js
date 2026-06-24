//react
import React from "react";
//third party
//style
import "./style.scss";
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";
import Button from "@material-ui/core/Button";
import Tooltip from "@material-ui/core/Tooltip";
import WarningRoundedIcon from "@material-ui/icons/WarningRounded";
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
      // row.warehouse is only set when every line shares one warehouse — null when the
      // outward spans multiple warehouses (open the row to see the per-line breakdown).
      let label = "—";
      if (row.warehouse) {
        label = row.warehouse.warehouseName;
      } else {
        const distinctNames = Array.from(new Set(
          (row.inwardOutwardList || [])
            .map((line) => line.warehouse && line.warehouse.warehouseName)
            .filter(Boolean)
        ));
        if (distinctNames.length > 0) label = `Multiple (${distinctNames.length})`;
      }
      return <td data-label={key}>{label}</td>;
    } else if (key === "inventoryCount") {
      return <td data-label={key}>{`${row["inwardOutwardList"].length}`}</td>;
    } else if (key === "usageLocation") {
      return <td data-label={messages.common.location}>{`${row["usageLocation"]["locationName"]}`}</td>;
    } else if (key === "usageArea") {
      return <td data-label={messages.common.finalLocation}>{`${row["usageArea"]["usageAreaName"]}`}</td>;
    } else if (key === "outwardid") {
      const noBOQ = row.hasBOQ !== true;
      const hasFifoOverride = row.hasFifoOverride === true;
      return (
        <td data-label='ID'>
          <Button
            color="primary"
            onClick={(e) => {
              e.stopPropagation();
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
          {noBOQ && (
            <Tooltip title="BOQ Bypassed — outward created without BOQ configured" arrow>
              <WarningRoundedIcon style={{ color: '#e65100', fontSize: '18px', verticalAlign: 'middle', marginLeft: '6px', cursor: 'default' }} />
            </Tooltip>
          )}
          {hasFifoOverride && (
            <Tooltip title="FIFO Override — batch selection was manually overridden" arrow>
              <WarningRoundedIcon style={{ color: '#1565c0', fontSize: '18px', verticalAlign: 'middle', marginLeft: '4px', cursor: 'default' }} />
            </Tooltip>
          )}
        </td>
      );
    } else {
      return super.renderCell(key, row,index);
    }
  }
}
export default Table;
