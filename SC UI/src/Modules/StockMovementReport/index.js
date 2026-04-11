import React from "react";
import AllInventory from "../AllInventory";
import { messages } from "../../messages";

const StockMovementReport = (props) => (
  <AllInventory
    {...props}
    pageTitle={messages.common.stockMovementReport}
    breadcrumbTitle={messages.common.inventory}
  />
);

export default StockMovementReport;
