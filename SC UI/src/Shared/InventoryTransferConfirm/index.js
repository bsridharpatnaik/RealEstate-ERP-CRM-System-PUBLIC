import React, { Component } from "react";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import Dialog from "@material-ui/core/Dialog";
import Button from "./../../Shared/Button";

class InventoryTransferConfirm extends Component {
  renderProductList() {
    const { transferData } = this.props;
    if (!transferData || !transferData.items) return null;

    return (
      <ul style={{ margin: '8px 0', paddingLeft: '20px' }}>
        {transferData.items.map((item, index) => {
          const product = transferData.products?.[item.productId];
          if (!product) return null;

          return (
            <li key={index} style={{ marginBottom: '4px' }}>
              {product.productName} - {item.quantity} {product.measurementUnit}
            </li>
          );
        })}
      </ul>
    );
  }

  render() {
    const { open, onCancel, onConfirm, transferData, submitting } = this.props;

    return (
      <Dialog
        disableBackdropClick
        disableEscapeKeyDown
        maxWidth="sm"
        aria-labelledby="inventory-transfer-dialog-title"
        open={open}
      >
        <DialogTitle id="inventory-transfer-dialog-title">Confirm Inventory Transfer</DialogTitle>
        <DialogContent dividers>
          <div style={{ marginBottom: '16px' }}>
            <strong>Below items should be transferred</strong>
          </div>
          <div style={{ marginBottom: '16px' }}>
            <strong>FROM:</strong> {transferData?.fromProjectName}, Warehouse - {transferData?.fromWarehouseName}
          </div>
          <div style={{ marginBottom: '16px' }}>
            <strong>TO:</strong> {transferData?.toProjectName}, Warehouse - {transferData?.toWarehouseName}
          </div>
          <div style={{ marginBottom: '16px' }}>
            <strong>Products:</strong>
            {this.renderProductList()}
          </div>
          <div>
            Are you sure you want to submit this transfer request?
          </div>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={onCancel}
            buttonClass="grey"
            label="No"
            disabled={submitting}
          ></Button>
          <Button
            onClick={onConfirm}
            buttonClass="blue"
            label={submitting ? "Submitting..." : "Yes"}
            disabled={submitting}
          ></Button>
        </DialogActions>
      </Dialog>
    );
  }
}

export default InventoryTransferConfirm;