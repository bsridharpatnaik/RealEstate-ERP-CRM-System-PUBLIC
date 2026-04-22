import React, { Component } from "react";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import Dialog from "@material-ui/core/Dialog";
import Button from "./../../Shared/Button";
import { withStyles } from "@material-ui/core/styles";
import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import CheckCircleIcon from "@material-ui/icons/CheckCircle";
import ErrorIcon from "@material-ui/icons/Error";

const StyledTableCell = withStyles((theme) => ({
  head: {
    backgroundColor: "#f5f5f5",
    fontWeight: "bold",
  },
  body: {
    fontSize: 14,
  },
}))(TableCell);

class InventoryTransferResultModal extends Component {
  renderItemResults() {
    const { itemResults, productLookup } = this.props;
    if (!itemResults || !Array.isArray(itemResults)) return null;

    return (
      <Table>
        <TableHead>
          <TableRow>
            <StyledTableCell>Product</StyledTableCell>
            <StyledTableCell align="center">Status</StyledTableCell>
            <StyledTableCell>Message</StyledTableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {itemResults.map((item, index) => {
            const product = productLookup?.[item.productId];
            const productName = product?.productName || `Product ID: ${item.productId}`;
            const isSuccess = item.success === true;
            
            return (
              <TableRow key={index}>
                <StyledTableCell>{productName}</StyledTableCell>
                <StyledTableCell align="center">
                  {isSuccess ? (
                    <CheckCircleIcon style={{ color: "#4caf50" }} />
                  ) : (
                    <ErrorIcon style={{ color: "#f44336" }} />
                  )}
                  <div style={{ fontSize: "12px", marginTop: "4px" }}>
                    {isSuccess ? "Success" : "Failure"}
                  </div>
                </StyledTableCell>
                <StyledTableCell>{item.message || "No message"}</StyledTableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    );
  }

  render() {
    const { open, onClose, itemResults } = this.props;

    return (
      <Dialog
        disableBackdropClick
        disableEscapeKeyDown={false}
        maxWidth="md"
        fullWidth
        aria-labelledby="inventory-transfer-result-dialog-title"
        open={open}
      >
        <DialogTitle id="inventory-transfer-result-dialog-title">
          Inventory Transfer Results
        </DialogTitle>
        <DialogContent dividers>
          {itemResults && itemResults.length > 0 ? (
            <div style={{ marginTop: "8px" }}>
              {this.renderItemResults()}
            </div>
          ) : (
            <div>No item results available.</div>
          )}
        </DialogContent>
        <DialogActions>
          <Button
            onClick={onClose}
            buttonClass="blue"
            label="Close"
          />
        </DialogActions>
      </Dialog>
    );
  }
}

export default InventoryTransferResultModal;
