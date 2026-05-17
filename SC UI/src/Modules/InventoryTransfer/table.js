//react
import React from "react";
//third party
import ArrowDropUpIcon from "@material-ui/icons/ArrowDropUp";
import ArrowDropDownIcon from "@material-ui/icons/ArrowDropDown";
import { withStyles } from "@material-ui/core/styles";
import Tooltip from "@material-ui/core/Tooltip";
import Typography from "@material-ui/core/Typography";

//import shared icons
//style
import "./style.scss";
//misc
import CommonTable from "./../../Shared/Table";
import { messages } from "./../../messages";

const HtmlTooltip = withStyles((theme) => ({
  tooltip: {
    backgroundColor: "#ffffff",
    color: "rgba(0, 0, 0, 0.87)",
    maxWidth: 300,
    fontSize: theme.typography.pxToRem(12),
    border: "1px solid #dadde9",
    borderRadius: "8px",
    boxShadow: "0 4px 8px rgba(0, 0, 0, 0.1)",
    padding: "12px",
  },
  arrow: {
    color: "#ffffff",
    "&::before": {
      border: "1px solid #dadde9",
    },
  },
}))(Tooltip);

// Shows ellipsis in the cell and only enables tooltip when text is actually truncated
const EllipsisTooltip = ({ text, minCharsForTooltip }) => {
  const spanRef = React.useRef(null);
  const [isTruncated, setIsTruncated] = React.useState(false);

  React.useEffect(() => {
    const el = spanRef.current;
    if (!el) return;

    const checkTruncation = () => {
      const node = spanRef.current;
      if (!node) return;
      const truncated = node.scrollWidth - node.clientWidth > 1; // small tolerance
      setIsTruncated(truncated);
    };

    // Defer to ensure layout is complete
    const rafId = window.requestAnimationFrame(checkTruncation);
    window.addEventListener("resize", checkTruncation);
    return () => {
      window.cancelAnimationFrame(rafId);
      window.removeEventListener("resize", checkTruncation);
    };
  }, [text]);

  const content = (
    <span ref={spanRef} className="it-remarks-text">
      {text}
    </span>
  );

  const shouldShowTooltip =
    isTruncated || (minCharsForTooltip && (text || "").length >= minCharsForTooltip);

  if (!shouldShowTooltip) {
    return content;
  }

  return (
    <HtmlTooltip
      title={
        <div style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
          {text}
        </div>
      }
      placement="top"
      arrow
    >
      {content}
    </HtmlTooltip>
  );
};

class Table extends CommonTable {
  renderHeader() {
    const headers = this.state.headers;
    return (
      <tr>
        {headers.map((header, index) => {
          const isInventoryCount = this.keys[index] === "inventoryCount";
          return (
            <th
              key={index}
              onClick={isInventoryCount ? undefined : () => this.sort(index)}
              className={isInventoryCount ? "" : "sortable"}
            >
              <span>
                {header} {this.renderSortIcon(index)}
              </span>
            </th>
          );
        })}
      </tr>
    );
  }

  renderCell(key, row, index) {
    if (key === "products") {
      const items = row.items || [];
      const productCount = items.length;

      if (productCount > 0) {
        const tooltipContent = (
          <div className="inventory-tooltip-content">
            <Typography className="inventory-tooltip-title">Product Details</Typography>
            <div className="inventory-tooltip-items">
              {items.map((item, idx) => {
                const productName = item.productName || "Unknown";
                const quantity = item.quantity || 0;
                const unit = item.measurementUnit || "";
                const productCode = item.productCode || "";
                return (
                  <div key={idx} className="inventory-tooltip-item">
                    <span className="inventory-tooltip-name">{productName}</span>
                    {productCode && <span className="inventory-tooltip-code">({productCode})</span>}
                    <span className="inventory-tooltip-quantity">{quantity} {unit}</span>
                  </div>
                );
              })}
            </div>
          </div>
        );

        return (
          <td data-label="Products">
            <HtmlTooltip title={tooltipContent} placement="top" arrow>
              <span className="inventory-count-hoverable">{productCount}</span>
            </HtmlTooltip>
          </td>
        );
      } else {
        return (
          <td data-label="Products">
            <span className="inventory-count-hoverable">0</span>
          </td>
        );
      }
    } else if (key === "fromProject") {
      return (
        <td data-label="From Project">
          <div className="project-cell">
            <div className="project-name">{row.fromProject || "-"}</div>
            <div className="warehouse-name">{row.fromWarehouse || "Warehouse name"}</div>
          </div>
        </td>
      );
    } else if (key === "toProject") {
      return (
        <td data-label="To Project">
          <div className="project-cell">
            <div className="project-name">{row.toProject || "-"}</div>
            <div className="warehouse-name">{row.toWarehouse || "Warehouse name"}</div>
          </div>
        </td>
      );
    } else if (key === "inventoryCount") {
      const count = row.inventoryCount || "";
      const unit = row.inventoryUnit || "";
      return (
        <td data-label="Inventory Count">
          {count} {unit}
        </td>
      );
    } else if (key === "creationDate") {
      // Format date if needed
      const date = row.creationDate || "";
      return <td data-label="Creation Date">{date}</td>;
    } else if (key === "sourceWarehouseName") {
      const value = row.sourceWarehouseName || "-";
      return (
        <td data-label="From Warehouse">
          <EllipsisTooltip text={value} minCharsForTooltip={18} />
        </td>
      );
    } else if (key === "targetWarehouseName") {
      const value = row.targetWarehouseName || "-";
      return (
        <td data-label="To Warehouse">
          <EllipsisTooltip text={value} minCharsForTooltip={18} />
        </td>
      );
    } else if (key === "remarks") {
      const remarks = row.remarks || "-";
      return (
        <td data-label="Remarks" className="it-remarks-cell">
          <EllipsisTooltip text={remarks} />
        </td>
      );
    } else {
      return super.renderCell(key, row, index);
    }
  }

  renderAction(row) {
    // No action buttons for inventory transfer list
    return null;
  }

  renderBody() {
    const rows = this.state.rows || [];
    const keys = this.state.keys;
    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={keys.length}>{messages.common.noRecords}</td>
        </tr>
      );
    }
    return rows.map((row, index) => (
      <tr
        key={index}
        className={index === rows.length - 1 ? "row last" : "row"}
      >
        {keys.map((key, index) => {
          return this.renderCell(key, row, index);
        })}
      </tr>
    ));
  }
}

export default Table;
