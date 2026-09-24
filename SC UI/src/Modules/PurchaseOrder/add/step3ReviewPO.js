import React, { Component } from "react";
import 'react-quill/dist/quill.core.css';

class Step3ReviewPO extends Component {
  formatCurrency(value) {
    const num = parseFloat(value);
    if (isNaN(num)) return "-";
    return num.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  formatValue(value) {
    return value && value !== "" ? value : "-";
  }

  computeLineItems() {
    const { items } = this.props;
    if (!items || items.length === 0) return [];

    const groupedItems = {};
    items.forEach((item) => {
      const productId = item.productId;
      if (!groupedItems[productId]) {
        groupedItems[productId] = {
          inventoryName: item.inventoryName,
          unit: item.unit || "",
          billingUnit: item.billingUnit || null,
          billingQuantity: item.billingQuantity || null,
          diameter: item.diameter || "",
          size: item.size || "",
          brandName: item.brandName || "",
          grade: item.grade || "",
          specification: item.specification || "",
          quantity: 0,
          rate: parseFloat(item.rate || 0),
          discount: parseFloat(item.discount || 0),
          tolerance: parseFloat(item.tolerance || 0),
          gst: parseFloat(item.gst || 0),
          sampleImageFileId: item.sampleImageFileId || null,
          sampleImagePreview: item.sampleImagePreview || null,
        };
      }
      groupedItems[productId].quantity += parseFloat(item.quantity || 0);
    });

    return Object.values(groupedItems).map((item) => {
      // Use billing qty for calculation when billing unit is active
      const qty = item.billingUnit && item.billingQuantity
        ? parseFloat(item.billingQuantity)
        : item.quantity;
      const rate = item.rate;
      const discount = item.discount;
      const gst = item.gst;
      const discountedRate = rate - (rate * discount) / 100;
      const netRate = discountedRate * qty;
      const totalAmt = netRate + (netRate * gst) / 100;
      // Display qty: billing qty with unit, else base qty
      const displayQty = item.billingUnit
        ? `${item.billingQuantity} ${item.billingUnit} (= ${item.quantity} ${item.unit})`
        : `${item.quantity} ${item.unit}`;
      return { ...item, qty, netRate, totalAmt, displayQty };
    });
  }

  formatAddress(obj) {
    if (!obj) return "-";
    const parts = [obj.addr_line1, obj.addr_line2, obj.city, obj.state, obj.zip].filter(Boolean);
    return parts.length > 0 ? parts.join(", ") : this.formatValue(obj.address);
  }

  render() {
    const { orderTo, orderFrom, poSubject, isSpecialPo, noteText, projectName, overridePhoneNumber, overrideEmail, fileInformations, freightCharges, freightGstPercent, customCharges, poDate, poDiscount } = this.props;
    const poDiscountAmt = parseFloat(poDiscount || 0);
    const lineItems = this.computeLineItems();
    const lineItemsTotal = lineItems.reduce((sum, item) => sum + (item.totalAmt || 0), 0);
    const fc = parseFloat(freightCharges || 0);
    const fgst = parseFloat(freightGstPercent !== "" && freightGstPercent !== undefined ? freightGstPercent : 18);
    const totalFreight = fc > 0 ? Math.round((fc + fc * fgst / 100) * 100) / 100 : 0;
    const customChargeRows = (customCharges || [])
      .filter(c => parseFloat(c.chargeAmount || 0) > 0)
      .map(c => {
        const amt = parseFloat(c.chargeAmount || 0);
        const gst = parseFloat(c.chargeGstPercent !== "" && c.chargeGstPercent !== undefined ? c.chargeGstPercent : 18);
        const total = Math.round((amt + amt * gst / 100) * 100) / 100;
        return { name: c.chargeName || "Additional Charges", amt, gst, total };
      });
    const totalCustom = customChargeRows.reduce((sum, c) => sum + c.total, 0);
    // PO Discount is a flat, post-tax PO-level deduction — applied once at the very end,
    // after line/freight/custom totals (which already include their own GST) are summed.
    const grandTotal = lineItemsTotal + totalFreight + totalCustom - poDiscountAmt;

    return (
      <div className="step3-review-po">
        {/* ── Supplier & Firm ── */}
        <div className="review-two-col">
          {/* Order To */}
          <div className="review-card">
            <div className="review-card-title">Order To (Supplier)</div>
            <div className="review-field-row">
              <span className="review-field-label">Name</span>
              <span className="review-field-value">{this.formatValue(orderTo?.name)}</span>
            </div>
            <div className="review-field-row">
              <span className="review-field-label">Contact Person</span>
              <span className="review-field-value">{this.formatValue(orderTo?.contactPerson)}</span>
            </div>
            <div className="review-field-row">
              <span className="review-field-label">Mobile</span>
              <span className="review-field-value">{this.formatValue(orderTo?.mobileNumber)}</span>
            </div>
            <div className="review-field-row">
              <span className="review-field-label">Address</span>
              <span className="review-field-value">{this.formatValue(orderTo?.address)}</span>
            </div>
          </div>

          {/* Order From */}
          <div className="review-card">
            <div className="review-card-title">Order From (Firm)</div>
            <div className="review-field-row">
              <span className="review-field-label">Name</span>
              <span className="review-field-value">{this.formatValue(orderFrom?.name)}</span>
            </div>
            <div className="review-field-row">
              <span className="review-field-label">GST</span>
              <span className="review-field-value">{this.formatValue(orderFrom?.gst)}</span>
            </div>
            <div className="review-field-row">
              <span className="review-field-label">PAN</span>
              <span className="review-field-value">{this.formatValue(orderFrom?.pan)}</span>
            </div>
            <div className="review-field-row">
              <span className="review-field-label">Contact</span>
              <span className="review-field-value">
                {overridePhoneNumber ? overridePhoneNumber : this.formatValue(orderFrom?.contactNumber)}
                {overridePhoneNumber && <span className="review-override-badge">overridden</span>}
              </span>
            </div>
            <div className="review-field-row">
              <span className="review-field-label">Address</span>
              <span className="review-field-value">{this.formatAddress(orderFrom)}</span>
            </div>
            {overrideEmail && (
              <div className="review-field-row">
                <span className="review-field-label">Email</span>
                <span className="review-field-value">
                  {overrideEmail}
                  <span className="review-override-badge">overridden</span>
                </span>
              </div>
            )}
          </div>
        </div>

        {/* ── PO Details ── */}
        <div className="review-card">
          <div className="review-card-title">PO Details</div>
          {poDate && (
            <div className="review-field-row">
              <span className="review-field-label">PO Date</span>
              <span className="review-field-value">{poDate}</span>
            </div>
          )}
          <div className="review-field-row">
            <span className="review-field-label">Subject</span>
            <span className="review-field-value">{this.formatValue(poSubject)}</span>
          </div>
          {projectName && (
            <div className="review-field-row">
              <span className="review-field-label">Project</span>
              <span className="review-field-value">{projectName}</span>
            </div>
          )}
          <div className="review-field-row">
            <span className="review-field-label">Special PO</span>
            <span className="review-field-value">{isSpecialPo ? "Yes" : "No"}</span>
          </div>
          {noteText && (
            <div className="review-field-row">
              <span className="review-field-label">Notes</span>
              <span
                className="review-field-value review-notes ql-editor"
                dangerouslySetInnerHTML={{ __html: noteText }}
              />
            </div>
          )}
        </div>

        {/* ── Attached Documents ── */}
        <div className="review-card">
          <div className="review-card-title">Attached Documents</div>
          {fileInformations && fileInformations.length > 0 ? (
            <div className="review-doc-list">
              {fileInformations.map((f, i) => (
                <div key={i} className="review-doc-item">
                  <span className="review-doc-icon">📄</span>
                  <span className="review-doc-name">{f.name || f.fileName || `File ${i + 1}`}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="review-no-docs">No documents attached</div>
          )}
        </div>

        {/* ── Line Items ── */}
        <div className="review-card review-items-card">
          <div className="review-card-title">Line Items</div>
          <div className="review-table-wrapper">
            <table className="review-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Inventory</th>
                  <th>Diameter</th>
                  <th>Size</th>
                  <th>Brand</th>
                  <th>Grade</th>
                  <th>Specification</th>
                  <th className="text-right">Qty</th>
                  <th className="text-right">Rate</th>
                  <th className="text-right">Discount %</th>
                  <th className="text-right">Tolerance %</th>
                  <th className="text-right">GST %</th>
                  <th className="text-right">Net Rate</th>
                  <th className="text-right">Total Amt</th>
                  <th>Sample Image</th>
                </tr>
              </thead>
              <tbody>
                {lineItems.map((item, index) => (
                  <tr key={index}>
                    <td data-label="#">{index + 1}</td>
                    <td data-label="Inventory" className="review-inventory-cell">
                      {item.inventoryName}
                      {item.unit && <span className="review-unit"> ({item.unit})</span>}
                    </td>
                    <td data-label="Diameter">{this.formatValue(item.diameter)}</td>
                    <td data-label="Size">{this.formatValue(item.size)}</td>
                    <td data-label="Brand">{this.formatValue(item.brandName)}</td>
                    <td data-label="Grade">{this.formatValue(item.grade)}</td>
                    <td data-label="Specification" className="review-spec-cell">{this.formatValue(item.specification)}</td>
                    <td data-label="Qty" className="text-right">{item.displayQty || item.quantity}</td>
                    <td data-label="Rate" className="text-right">{this.formatCurrency(item.rate)}</td>
                    <td data-label="Discount %" className="text-right">{item.discount > 0 ? `${item.discount}%` : "-"}</td>
                    <td data-label="Tolerance %" className="text-right">{item.tolerance > 0 ? `${item.tolerance}%` : "-"}</td>
                    <td data-label="GST %" className="text-right">{item.gst > 0 ? `${item.gst}%` : "-"}</td>
                    <td data-label="Net Rate" className="text-right">{this.formatCurrency(item.netRate)}</td>
                    <td data-label="Total Amt" className="text-right review-total-cell">{this.formatCurrency(item.totalAmt)}</td>
                    <td data-label="Sample Image" style={{ textAlign: "center" }}>
                      {item.sampleImageFileId ? (
                        <img
                          src={item.sampleImagePreview}
                          alt="sample"
                          style={{ width: 56, height: 56, objectFit: "contain", border: "1px solid #ddd", borderRadius: 4 }}
                        />
                      ) : "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {fc > 0 && (
            <div style={{ textAlign: "right", padding: "8px 8px 0", fontSize: "13px", color: "#323c47", borderTop: "1px solid #e0e0e0", marginTop: "4px" }}>
              <div style={{ marginBottom: "3px" }}>
                Freight Charges: <strong>₹ {this.formatCurrency(fc)}</strong>
                <span style={{ margin: "0 12px", color: "#999" }}>|</span>
                GST ({this.formatCurrency(fgst)}%): <strong>₹ {this.formatCurrency(fc * fgst / 100)}</strong>
                <span style={{ margin: "0 12px", color: "#999" }}>|</span>
                Total Freight: <strong>₹ {this.formatCurrency(totalFreight)}</strong>
              </div>
            </div>
          )}
          {customChargeRows.map((cc, idx) => (
            <div key={idx} style={{ textAlign: "right", padding: "8px 8px 0", fontSize: "13px", color: "#323c47", borderTop: "1px solid #e0e0e0", marginTop: "4px" }}>
              <div style={{ marginBottom: "3px" }}>
                {cc.name}: <strong>₹ {this.formatCurrency(cc.amt)}</strong>
                <span style={{ margin: "0 12px", color: "#999" }}>|</span>
                GST ({this.formatCurrency(cc.gst)}%): <strong>₹ {this.formatCurrency(cc.amt * cc.gst / 100)}</strong>
                <span style={{ margin: "0 12px", color: "#999" }}>|</span>
                Total: <strong>₹ {this.formatCurrency(cc.total)}</strong>
              </div>
            </div>
          ))}
          {poDiscountAmt > 0 && (
            <div style={{ textAlign: "right", padding: "8px 8px 0", fontSize: "13px", color: "#323c47", borderTop: "1px solid #e0e0e0", marginTop: "4px" }}>
              <div style={{ marginBottom: "3px" }}>
                PO Discount: <strong>- ₹ {this.formatCurrency(poDiscountAmt)}</strong>
              </div>
            </div>
          )}
          <div className="review-grand-total">
            Grand Total: <strong>₹ {this.formatCurrency(grandTotal)}</strong>
          </div>
        </div>
      </div>
    );
  }
}

export default Step3ReviewPO;
