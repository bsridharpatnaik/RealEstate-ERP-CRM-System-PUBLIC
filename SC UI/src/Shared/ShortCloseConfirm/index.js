import React, { Component } from "react";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import Dialog from "@material-ui/core/Dialog";
import TextField from "@material-ui/core/TextField";
import Button from "./../../Shared/Button";

class ShortCloseConfirm extends Component {
  state = {
    reason: "",
    error: "",
  };

  handleReasonChange = (event) => {
    this.setState({ reason: event.target.value, error: "" });
  };

  handleConfirm = () => {
    const { reason } = this.state;
    if (!reason || reason.trim().length === 0) {
      this.setState({ error: "Reason is required" });
      return;
    }

    if (reason.trim().length < 3) {
      this.setState({ error: "Reason must be at least 3 characters long" });
      return;
    }

    this.props.onConfirm(reason.trim());
  };

  handleCancel = () => {
    this.setState({ reason: "", error: "" });
    this.props.onCancel();
  };

  render() {
    const { open, purchaseOrderNo } = this.props;
    const { reason, error } = this.state;

    return (
      <Dialog
        disableBackdropClick
        disableEscapeKeyDown
        maxWidth="sm"
        aria-labelledby="short-close-dialog-title"
        open={open}
      >
        <DialogTitle id="short-close-dialog-title">
          Short Close Purchase Order
        </DialogTitle>
        <DialogContent dividers>
          <div style={{ marginBottom: '16px' }}>
            <strong>Purchase Order:</strong> {purchaseOrderNo}
          </div>
          <div style={{ marginBottom: '16px' }}>
            Are you sure you want to short close this purchase order?
          </div>
          <TextField
            autoFocus
            margin="dense"
            label="Reason for Short Close"
            type="text"
            fullWidth
            multiline
            rows={3}
            value={reason}
            onChange={this.handleReasonChange}
            error={!!error}
            helperText={error}
            variant="outlined"
            placeholder="Please provide a reason for short closing this purchase order..."
          />
        </DialogContent>
        <DialogActions>
          <Button
            onClick={this.handleCancel}
            buttonClass="grey"
            label="Cancel"
          />
          <Button
            onClick={this.handleConfirm}
            buttonClass="blue"
            label="Short Close"
            disabled={!reason.trim()}
          />
        </DialogActions>
      </Dialog>
    );
  }
}

export default ShortCloseConfirm;