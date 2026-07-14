import React, { Component } from "react";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";
import Table from "@material-ui/core/Table";
import TableHead from "@material-ui/core/TableHead";
import TableBody from "@material-ui/core/TableBody";
import TableRow from "@material-ui/core/TableRow";
import TableCell from "@material-ui/core/TableCell";
import Paper from "@material-ui/core/Paper";
import Chip from "@material-ui/core/Chip";
import "./style.scss";

class TransferDetails extends Component {
  state = {
    data: null,
    loading: true,
    error: null,
  };

  async componentDidMount() {
    const { row } = this.props;
    if (!row || !row.transferId) {
      this.setState({ loading: false, error: "No transfer ID provided." });
      return;
    }
    const r = await API.GET(apiEndpoints.getInventoryTransferById(row.transferId));
    if (r.success) {
      this.setState({ data: r.data, loading: false });
    } else {
      this.setState({ loading: false, error: "Failed to load transfer details." });
    }
  }

  renderField(label, value) {
    return (
      <div style={{ display: "flex", marginBottom: 8 }}>
        <span style={{ fontWeight: 600, minWidth: 180, color: "#555" }}>{label}</span>
        <span>{value || "—"}</span>
      </div>
    );
  }

  renderBatchChip(item) {
    if (!item.batchTracked) return null;
    return (
      <Chip
        label="Batch Tracked"
        size="small"
        style={{ backgroundColor: "#e3f2fd", color: "#1565c0", fontSize: 11, marginLeft: 6 }}
      />
    );
  }

  render() {
    const { back } = this.props;
    const { data, loading, error } = this.state;

    if (loading) {
      return (
        <div className="details-loader" style={{ padding: 32, color: "#888" }}>
          Loading transfer details…
        </div>
      );
    }

    if (error || !data) {
      return (
        <div style={{ padding: 32, color: "#c62828" }}>
          {error || "Could not load transfer."}
        </div>
      );
    }

    const items = data.items || [];

    return (
      <div className="details-container" style={{ padding: "16px 24px" }}>
        {/* Back button */}
        <div
          style={{ display: "flex", alignItems: "center", marginBottom: 16, cursor: "pointer", color: "#1565c0" }}
          onClick={back}
        >
          <KeyboardBackspaceIcon style={{ marginRight: 6 }} />
          <span style={{ fontWeight: 600 }}>Back to Transfers</span>
        </div>

        <h3 style={{ marginBottom: 16, color: "#333" }}>
          Transfer #{data.transferId}
        </h3>

        {/* Header info */}
        <Paper elevation={1} style={{ padding: "16px 20px", marginBottom: 20 }}>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "8px 32px" }}>
            {this.renderField("Transfer Date", data.transferDate)}
            {this.renderField("From Project", data.sourceTenant)}
            {this.renderField("To Project", data.targetTenant)}
            {this.renderField("From Warehouse", data.sourceWarehouseName)}
            {this.renderField("To Warehouse", data.targetWarehouseName)}
            {data.remarks && this.renderField("Remarks", data.remarks)}
          </div>
        </Paper>

        {/* Items table */}
        <h4 style={{ marginBottom: 10, color: "#333" }}>Transferred Items</h4>
        <Paper elevation={1}>
          <Table size="small">
            <TableHead>
              <TableRow style={{ backgroundColor: "#2E5496" }}>
                {["Product", "Code", "Unit", "Qty Transferred", "Source Closing Stock", "Dest. Closing Stock"].map(
                  (h) => (
                    <TableCell key={h} style={{ color: "#fff", fontWeight: 600, padding: "10px 14px" }}>
                      {h}
                    </TableCell>
                  )
                )}
              </TableRow>
            </TableHead>
            <TableBody>
              {items.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} style={{ textAlign: "center", color: "#888", padding: 20 }}>
                    No items found.
                  </TableCell>
                </TableRow>
              ) : (
                items.map((item, idx) => (
                  <TableRow key={item.transferItemId || idx} style={{ backgroundColor: idx % 2 === 0 ? "#fff" : "#f9f9f9" }}>
                    <TableCell style={{ padding: "8px 14px" }}>
                      {item.productName}
                    </TableCell>
                    <TableCell style={{ padding: "8px 14px", color: "#666" }}>
                      {item.productCode}
                    </TableCell>
                    <TableCell style={{ padding: "8px 14px", color: "#666" }}>
                      {item.measurementUnit}
                    </TableCell>
                    <TableCell style={{ padding: "8px 14px", fontWeight: 600 }}>
                      {item.quantity}
                    </TableCell>
                    <TableCell style={{ padding: "8px 14px", color: "#666" }}>
                      {item.sourceClosingStock != null ? item.sourceClosingStock : "—"}
                    </TableCell>
                    <TableCell style={{ padding: "8px 14px", color: "#666" }}>
                      {item.targetClosingStock != null ? item.targetClosingStock : "—"}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Paper>

        <div
          style={{
            marginTop: 16,
            padding: "10px 14px",
            backgroundColor: "#e3f2fd",
            borderLeft: "4px solid #1565c0",
            borderRadius: 4,
            fontSize: 13,
            color: "#1565c0",
          }}
        >
          For batch-tracked products, batches were transferred to the destination warehouse preserving all
          batch metadata (brand, lot number, expiry date). View current batch details via Stock → Batches tab.
        </div>
      </div>
    );
  }
}

export default TransferDetails;
