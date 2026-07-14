import React, { Component } from "react";
import Button from "./../../Shared/Button";
import CircularProgress from "@material-ui/core/CircularProgress";
import "../PurchaseOrder/add/stepStyles.scss";

class Step2ReviewIndent extends Component {

  formatValue(val) {
    if (val === null || val === undefined || val === "") return "-";
    return val;
  }

  renderLineItemsTable() {
    const { noinventory } = this.props;
    const items = Object.values(noinventory || {}).filter(
      (item) => item && item.productId
    );

    if (items.length === 0) {
      return <div className="review-empty">No inventory items added.</div>;
    }

    return (
      <table className="indent-review-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Category</th>
            <th>Product Name</th>
            <th className="text-center">Quantity</th>
            <th>Unit</th>
            <th>Specification</th>
            <th>Remarks</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item, index) => (
            <tr key={index}>
              <td>{index + 1}</td>
              <td>{this.formatValue(item.selectedCategory?.name)}</td>
              <td>
                {this.formatValue(item.selectedProduct?.name)}
                {item.leadTimeDays != null && (
                  <div style={{ fontSize: 11, color: "#888", marginTop: 2 }}>
                    ⏱ Lead: <strong>{item.leadTimeDays}d</strong>
                  </div>
                )}
              </td>
              <td className="text-center">{this.formatValue(item.quantity)}</td>
              <td>{this.formatValue(item.unit)}</td>
              <td>{this.formatValue(item.specification)}</td>
              <td>{this.formatValue(item.remarks)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  renderAttachedFiles() {
    const files = this.props.fileInformations || [];
    if (files.length === 0) return null;

    return (
      <div className="review-card">
        <div className="review-card-title">Attached Documents</div>
        <div className="review-card-body">
          {files.map((f, i) => (
            <div key={i} className="review-field-row">
              <span className="review-field-label">File {i + 1}</span>
              <span className="review-field-value">{f.fileName || f.name || "-"}</span>
            </div>
          ))}
        </div>
      </div>
    );
  }

  render() {
    const { onBack, onConfirm, isSaving, indentDate, requiredBy } = this.props;

    return (
      <div className="step3-review-po indent-review-wrapper">
        <div className="review-section-title">Review Indent</div>
        <p className="review-section-subtitle">
          Please verify the details below before confirming.
        </p>

        {/* Indent meta info */}
        <div className="review-card">
          <div className="review-card-title">Indent Details</div>
          <div className="review-card-body">
            <div className="review-field-row">
              <span className="review-field-label">Indent Date</span>
              <span className="review-field-value">{this.formatValue(indentDate)}</span>
            </div>
            <div className="review-field-row">
              <span className="review-field-label">Required By</span>
              <span className="review-field-value">{this.formatValue(requiredBy)}</span>
            </div>
          </div>
        </div>

        {/* Line items */}
        <div className="review-card">
          <div className="review-card-title">Inventory Items</div>
          <div className="review-card-body review-table-wrapper">
            {this.renderLineItemsTable()}
          </div>
        </div>

        {/* Attached files */}
        {this.renderAttachedFiles()}

        {/* Action buttons */}
        <div className="review-actions">
          <Button
            buttonClass="grey"
            label="← Back to Edit"
            onClick={onBack}
            disabled={isSaving}
          />
          <Button
            buttonClass="blue"
            label={isSaving ? "Saving..." : "Confirm & Save"}
            onClick={onConfirm}
            disabled={isSaving}
          />
          {isSaving && (
            <CircularProgress size={20} style={{ marginLeft: 12, color: "#1976d2" }} />
          )}
        </div>
      </div>
    );
  }
}

export default Step2ReviewIndent;
