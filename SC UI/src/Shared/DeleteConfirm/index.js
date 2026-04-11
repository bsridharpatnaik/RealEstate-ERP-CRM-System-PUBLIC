import React, { Component } from "react";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import Dialog from "@material-ui/core/Dialog";
import Button from "./../../Shared/Button";

class DeleteConfirm extends Component {
  render() {
    return (
      <Dialog
        disableBackdropClick
        disableEscapeKeyDown
        maxWidth="xs"
        aria-labelledby="confirmation-dialog-title"
        open={this.props.open}
      >
        <DialogTitle id="confirmation-dialog-title">Confirm</DialogTitle>
        <DialogContent dividers>
          Are you sure you want to delete this record?
        </DialogContent>
        <DialogActions>
          <Button
            onClick={this.props.onCancel}
            buttonClass="grey"
            label="Cancel"
          ></Button>
          <Button
            onClick={this.props.onConfirm}
            buttonClass="blue"
            label="Confirm"
          ></Button>
        </DialogActions>
      </Dialog>
    );
  }
}

class InventoryTransferConfirm extends Component {
  renderProductList() {
    const { transferData } = this.props;
    if (!transferData || !transferData.items) return null;

    return transferData.items.map((item, index) => {
      const product = transferData.products?.[item.productId];
      if (!product) return null;

      return (
        <div key={index} style={{ marginBottom: '8px' }}>
          {product.productName} - {item.quantity} {product.measurementUnit}
        </div>
      );
    });
  }

  render() {
    const { open, onCancel, onConfirm, transferData } = this.props;

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
            <div style={{ marginTop: '8px', paddingLeft: '16px' }}>
              {this.renderProductList()}
            </div>
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
          ></Button>
          <Button
            onClick={onConfirm}
            buttonClass="blue"
            label="Yes"
          ></Button>
        </DialogActions>
      </Dialog>
    );
  }
}

export default DeleteConfirm;
export { InventoryTransferConfirm };